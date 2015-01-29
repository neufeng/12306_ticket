package com.free.app.ticket.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.free.app.ticket.TicketMainFrame;
import com.free.app.ticket.model.ContacterInfo;
import com.free.app.ticket.model.JsonMsg4Authcode;
import com.free.app.ticket.model.JsonMsg4CheckOrder;
import com.free.app.ticket.model.JsonMsg4ConfirmQueue;
import com.free.app.ticket.model.JsonMsg4Contacter;
import com.free.app.ticket.model.JsonMsg4LeftTicket;
import com.free.app.ticket.model.JsonMsg4Login;
import com.free.app.ticket.model.JsonMsg4QueryTicket;
import com.free.app.ticket.model.JsonMsg4QueryTicket.TicketQueryInfo;
import com.free.app.ticket.model.JsonMsg4QueryWait;
import com.free.app.ticket.model.JsonMsg4QueueCount;
import com.free.app.ticket.model.JsonMsg4SubmitOrder;
import com.free.app.ticket.model.JsonMsgSuper;
import com.free.app.ticket.model.PassengerData;
import com.free.app.ticket.model.TicketConfigInfo;
import com.free.app.ticket.model.TrainInfo;
import com.free.app.ticket.model.JsonMsg4LeftTicket.TrainQueryInfo;
import com.free.app.ticket.model.PassengerData.SeatType;
import com.free.app.ticket.service.AutoBuyThreadService.OrderToken;
import com.free.app.ticket.util.constants.HttpHeader;
import com.free.app.ticket.util.constants.UrlConstants;

/**
 * @author steven
 *
 */
/**
 * @author steven
 *
 */
public class TicketHttpClient {
    
    private static final Logger logger = LoggerFactory.getLogger(TicketHttpClient.class);
    
    private String JSESSIONID = null;
    
    private String BIGipServerotn = null;
    
    private String current_captcha_type = null;
    
    private String loginKey = null;
    
    private String orderTicketKey = null;
    
    private String checkOrderKey = null;
    
    private static final int DEBBUG_MAX_COUNT = 1000;
    
    private String query_url = UrlConstants.GET_TICKET_QUERY_URL;
    
    private static final Pattern PATTERN_DYNAMIC_JS = Pattern.compile("/otn/dynamicJs/(\\w+)");
    
    private static final Pattern PATTERN_LOGIN_KEY = Pattern.compile("var key='(\\w+)'");
    
    /*
     * private static final String loginAuthCodeFilePath =
     * System.getProperty("user.dir") + File.separator + "login_authcode.jpg";
     */
    
    /* loginUrl = path + "image" + File.separator + "passcode-login.jpg"; */
    
    private TicketHttpClient() {
        getCookies();
    }
    
    /**
     * <初始获取cookie>
     * @return
     */
    public static TicketHttpClient getInstance() {
        TicketHttpClient instance = new TicketHttpClient();
        
        instance.loginInit();
        
        return instance;
    }
    
    /**
     * 获取cookies
     */
    private void getCookies() {
        if (logger.isDebugEnabled()) {
            logger.debug("获取cookies");
            logger.debug("[get url] {}", UrlConstants.GET_BASE_URL);
        }
        
        HttpGet get = new HttpGet(UrlConstants.GET_BASE_URL);
        HttpHeader.setCommonHeader(get);
        
        try {
            Header[] headers = doGetHeaderRequest(get);
            updateSessionId(headers);
        }
        catch (Exception e) {
            logger.error("getCookies error : ", e);
        }
    }
    
    /**
     * 更新JSESSIONID,BIGipServerotn,current_captcha_type
     * @param headers
     */
    private void updateSessionId(Header[] headers) {
        if (headers == null) {
            return;
        }
        
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].getName().equals("Set-Cookie")) {
                String cookie[] = headers[i].getValue().split("=");
                String cookieName = cookie[0];
                String cookieValue = cookie[1].split(";")[0];
                if (cookieName.equals("JSESSIONID")) {
                    JSESSIONID = cookieValue;
                }
                if (cookieName.equals("BIGipServerotn")) {
                    BIGipServerotn = cookieValue;
                }
                if (cookieName.equals("current_captcha_type")) {
                    current_captcha_type = cookieValue;
                }
            }
        }
        
        logger.debug("JSESSIONID Changed");
        logger.debug("JSESSIONID = " + JSESSIONID + ";BIGipServerotn = " + BIGipServerotn + ";current_captcha_type = " + current_captcha_type);
    }
    
    /**
     * 登录动态参数
     */
    private void loginInit() {
        HttpGet get = new HttpGet(UrlConstants.GET_LOGIN_INIT_URL);
        HttpHeader.setLoginInitHeader(get);
        setCookie(get, null);
        
        String loginDynamicJs = null;
        try {
            String result = doGetRequest(get);
            
            Matcher m_token = PATTERN_DYNAMIC_JS.matcher(result);
            if (m_token.find()) {
                loginDynamicJs = m_token.group(1);
            }
            else {
                logger.error("httpClient init get loginDynamicJsUrl  fail for unknow reason, check it!");
            }
        }
        catch (Exception e) {
            logger.error("获取登录key出错", e);
        }
        
        if (loginDynamicJs != null) {
            this.loginKey = getRandomParamKey(loginDynamicJs);
        }
    }
    
    /**
     * 获取动态参数
     * @param jsFileName
     * @return
     */
    private String getRandomParamKey(String jsFileName) {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax get 获取dynamicJs的内容，从中提取出key---");
        }
        
        String dynamicJsUrl = UrlConstants.GET_DYNAMIC_JS_URL + jsFileName;
        HttpGet get = new HttpGet(dynamicJsUrl);
        HttpHeader.setCommonHeader(get);
        setCookie(get, null);
        
        String key = null;
        try {
            String result = doGetRequest(get);
            
            clearKeyTimmer(result);
            
            Matcher m_token = PATTERN_LOGIN_KEY.matcher(result);
            if (m_token.find()) {
                key = m_token.group(1);
                System.out.println("key " + key);
            }
        }
        catch (Exception e) {
            logger.error("获取key出错", e);
            TicketMainFrame.remind("获取key出错");
        }
        
        return key;
    }
    
    /**
     * 清除key失效时间
     * @param jsStr
     */
    private void clearKeyTimmer(String jsStr) {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax post 清除key失效时间---");
        }
        
        Matcher m_token = PATTERN_DYNAMIC_JS.matcher(jsStr);
        String jsFileName = null;
        if (m_token.find()) {
            jsFileName = m_token.group(1);
        }
        if (jsFileName == null) {
            logger.error("清除key失效时间出错");
        }
        
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("_json_att", ""));
        
        String dynamicJsUrl = "https://kyfw.12306.cn/otn/dynamicJs/" + jsFileName;
        HttpPost post = new HttpPost(dynamicJsUrl);
        HttpHeader.setPostAjaxHeader(post);
        setCookie(post, null);
        
        try {
            doPostRequest(post, params);
        }
        catch (Exception e) {
            logger.error("清除key失效时间出错", e);
        }
    }
    
    private File buildCodeImage(String url, String suffix) {
        HttpClient httpclient = buildHttpClient();
        HttpGet get = new HttpGet(url);
        HttpHeader.setLoginAuthCodeHeader(get);
        setCookie(get, null);
        
        File file = new File(System.getProperty("user.dir") + File.separator + "passcode" + File.separator + System.currentTimeMillis() + suffix);
        OutputStream out = null;
        InputStream is = null;
        try {
            HttpResponse response = httpclient.execute(get);
            Header[] headers = response.getAllHeaders();
            updateSessionId(headers);
            
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                is = entity.getContent();
                out = new FileOutputStream(file);
                int readByteCount = -1;
                byte[] buffer = new byte[256];
                while ((readByteCount = is.read(buffer)) != -1) {
                    out.write(buffer, 0, readByteCount);
                }
            }
        }
        catch (Exception e) {
            file = null;
            logger.error("buildCodeImage error : ", e);
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                }
                catch (IOException e) {
                }
            }
            httpclient.getConnectionManager().shutdown();
        }
        
        return file;
    }
    
    public File buildLoginCodeImage() {
        if (logger.isDebugEnabled()) {
            logger.debug("获取登录验证码");
            logger.debug("[get url] {}", UrlConstants.GET_LOGIN_PASSCODE_URL);
        }
        
        return buildCodeImage(UrlConstants.GET_LOGIN_PASSCODE_URL, ".login.png");
    }
    
    public boolean checkLoginAuthcode(String authCode) {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax post 检查验证码---");
        }
        
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("randCode", authCode));
        params.add(new BasicNameValuePair("rand", "sjrand"));
        params.add(new BasicNameValuePair("randCode_validate", ""));
        
        HttpPost post = new HttpPost(UrlConstants.REQ_CHECK_CODE_URL);
        HttpHeader.setPostAjaxHeader(post);
        setCookie(post, null);
        
        boolean result = false;
        try {
            String checkResult = doPostRequest(post, params);
            JsonMsg4Authcode msg = JSONObject.parseObject(checkResult, JsonMsg4Authcode.class);
            if ("1".equals(msg.getData().getResult())) {
                result = true;
            }
        }
        catch (Exception e) {
            logger.error("验证码检查发生异常", e);
            TicketMainFrame.remind("验证码检查发生异常,请联系管理员");
        }
        
        return result;
    }
    
    /**
     * <登录检查>
     * 
     * @param username
     * @param password
     * @param authcode
     * @return null代表成功，否则返回登录失败原因
     */
    public String checkLogin(String username, String password, String authcode) {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax post 登录检查---");
        }
        boolean checkCodeResult = checkLoginAuthcode(authcode);// 先检查验证码
        if (!checkCodeResult) {
            return "验证码不正确！";
        }
        
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("loginUserDTO.user_name", username));
        params.add(new BasicNameValuePair("userDTO.password", password));
        params.add(new BasicNameValuePair("randCode", authcode));
        params.add(new BasicNameValuePair("randCode_validate", ""));
        if (loginKey != null) {
            params.add(new BasicNameValuePair(loginKey, DynamicJsUtil.getRandomParamValue(loginKey)));
        }
        else {
            TicketMainFrame.remind("尚未生成登录key，请稍后...");
        }
        params.add(new BasicNameValuePair("myversion", "undefined"));
        
        HttpPost post = new HttpPost(UrlConstants.REQ_LOGIN_AYSN_SUGGEST_URL);
        HttpHeader.setPostAjaxHeader(post);
        setCookie(post, null);
        
        String result = null;
        try {
            String checkResult = doPostRequest(post, params);
            JsonMsg4Login msg = JSONObject.parseObject(checkResult, JsonMsg4Login.class);
            if (!msg.getStatus()) {
                result = msg.getMessages()[0];
            }
            else {
                String loginCheck = msg.getData().getString("loginCheck");
                if (loginCheck == null || !loginCheck.equals("Y")) {// 登录不成功
                    result = msg.getMessages()[0];
                }
                else {
                    login(); //登录
                }
            }
        }
        catch (Exception e) {
            result = "登录检查异常!";
            logger.error("登录检查发生异常", e);
            TicketMainFrame.remind("登录检查发生异常");
        }
        
        return result;
    }
    
    public void login() {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax post 登录---");
        }
        
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("_json_att", ""));
        
        HttpPost post = new HttpPost(UrlConstants.REQ_LOGIN_URL);
        HttpHeader.setPostAjaxHeader(post);
        setCookie(post, null);
        
        try {
            doPostRequest(post, params);
        }
        catch (Exception e) {
            logger.error("登录发生异常", e);
            TicketMainFrame.remind("登录发生异常");
            return;
        }
        
        initMy12306();
    }
    
    private void initMy12306() {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax get 初始化我的12306---");
        }
        HttpGet get = new HttpGet(UrlConstants.GET_INIT_MY_12306_URL);
        HttpHeader.setGetAjaxHeader(get);
        setCookie(get, null);
        
        try {
            doGetRequest(get);
        }
        catch (Exception e) {
            logger.error("初始化我的12306异常", e);
        }
    }
    
    public ContacterInfo[] getPassengers() {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax post 获取联系人---");
        }
        HttpPost post = new HttpPost(UrlConstants.REQ_PASSENGERS_QUERY_URL);
        HttpHeader.setPostAjaxHeader(post);
        setCookie(post, null);
        
        ContacterInfo[] result = null;
        try {
            String checkResult = doPostRequest(post, null);
            JsonMsg4Contacter msg = JSONObject.parseObject(checkResult, JsonMsg4Contacter.class);
            if (msg.getStatus()) {
                if (!msg.getData().isExist() && msg.getData().getExMsg() != null) {
                    TicketMainFrame.remind(msg.getData().getExMsg());
                }
                else {
                    result = msg.getData().getNormal_passengers();
                }
            }
        }
        catch (Exception e) {
            logger.error("获取联系人异常", e);
            TicketMainFrame.remind("获取联系人异常,请稍后再试");
        }
        
        return result;
    }
    
    /**
     * <查询余票信息>
     * @param configInfo
     * @param cookieMap
     * @return
     */
    public List<TrainInfo> queryLeftTicket(TicketConfigInfo configInfo, Map<String, String> cookieMap) {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax get 查询余票信息---");
        }
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("leftTicketDTO.train_date", configInfo.getTrain_date()));
        params.add(new BasicNameValuePair("leftTicketDTO.from_station", configInfo.getFrom_station()));
        params.add(new BasicNameValuePair("leftTicketDTO.to_station", configInfo.getTo_station()));
        params.add(new BasicNameValuePair("purpose_codes", configInfo.getPurpose_codes()));
        String paramsUrl = URLEncodedUtils.format(params, "UTF-8");
        
        if ("A".equals(current_captcha_type) || "C".equals(current_captcha_type)) {//验证码为A类型时，查询前先调用LOG请示
            queryLog(paramsUrl, cookieMap);
        }
        HttpGet get = new HttpGet(query_url + "?" + paramsUrl);
        HttpHeader.setGetAjaxHeader(get);
        setCookie(get, cookieMap);
        
        List<TrainInfo> result = null;
        try {
            String checkResult = doGetRequest(get);
            JsonMsg4LeftTicket msg = JSONObject.parseObject(checkResult, JsonMsg4LeftTicket.class);
            if (msg.getStatus()) {
                List<TrainQueryInfo> infos = msg.getData();
                if (infos != null) {
                    result = new ArrayList<TrainInfo>();
                    TrainInfo trainInfo;
                    for (TrainQueryInfo info : infos) {
                        trainInfo = info.getQueryLeftNewDTO();
                        trainInfo.setSecretStr(info.getSecretStr());
                        result.add(trainInfo);
                    }
                }
                else {
                    String[] tips = msg.getMessages();
                    if (tips != null && tips.length > 0) {
                        if (tips[0].contains("非法")) {
                            logger.error("查询余票时出现错误：" + tips[0]);
                        }
                    }
                }
            }
            else {
                if (StringUtils.isNotEmpty(msg.getC_url())) {//切换地址了
                    query_url = UrlConstants.GET_BASE_URL + msg.getC_url();
                }
            }
        }
        catch (JSONException e) {
            query_url = UrlConstants.GET_TICKET_QUERY_URL2;
            logger.error("查询余票异常，内容异常", e);
            TicketMainFrame.remind("查询余票异常，内容异常");
        }
        catch (Exception e) {
            logger.error("查询余票异常", e);
            TicketMainFrame.remind("查询余票异常");
        }
        
        return result;
    }
    public List<TrainInfo> queryLeftTicket(TicketConfigInfo configInfo) {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax get 查询余票信息---");
        }
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("purpose_codes", configInfo.getPurpose_codes()));
        params.add(new BasicNameValuePair("queryDate", configInfo.getTrain_date()));
        params.add(new BasicNameValuePair("from_station", configInfo.getFrom_station()));
        params.add(new BasicNameValuePair("to_station", configInfo.getTo_station()));
        String paramsUrl = URLEncodedUtils.format(params, "UTF-8");
        
        HttpGet get = new HttpGet(UrlConstants.GET_TICKET_QUERY_URL3 + "?" + paramsUrl);
        HttpHeader.setGetAjaxHeader(get);
        setCookie(get, null);
        
        List<TrainInfo> result = null;
        try {
            String checkResult = doGetRequest(get);
            JsonMsg4QueryTicket msg = JSONObject.parseObject(checkResult, JsonMsg4QueryTicket.class);
            if (msg.getStatus()) {
                TicketQueryInfo info = msg.getData();
                if (info != null) {
                    result = info.getDatas();
                }
            }
        }
        catch (JSONException e) {
            logger.error("查询余票异常，内容异常", e);
            TicketMainFrame.remind("查询余票异常，内容异常");
        }
        catch (Exception e) {
            logger.error("查询余票异常", e);
            TicketMainFrame.remind("查询余票异常");
        }
        
        return result;
    }
    
    /**
     * 查询余票初始化
     */
    public void queryLeftTicketInit() {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax get 查询余票初始化---");
        }
        
        HttpGet get = new HttpGet(UrlConstants.GET_TICKET_QUERY_INIT);
        HttpHeader.setGetAjaxHeader(get);
        setCookie(get, null);
        
        try {
            doGetRequest(get);
        }
        catch (Exception e) {
            logger.error("查询余票初始化异常", e);
        }
    }
    
    /**
     * <查询余票前先进行LOG调用>
     * @param paramsUrl
     * @param cookieMap
     */
    public void queryLog(String paramsUrl, Map<String, String> cookieMap) {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax get 查询余票前先LOG---");
        }
        
        HttpGet get = new HttpGet(UrlConstants.GET_TICKET_QUERY_LOG_URL + "?" + paramsUrl);
        HttpHeader.setGetAjaxHeader(get);
        setCookie(get, cookieMap);
        
        try {
            String checkResult = doGetRequest(get);
            JsonMsgSuper msg = JSONObject.parseObject(checkResult, JsonMsgSuper.class);
            if (msg.getStatus()) {
                
            }
        }
        catch (Exception e) {
            logger.error("查询余票前LOG异常", e);
        }
        
    }
    
    /**
     * 订单提交前获取动态参数
     * @param cookieMap
     */
    public void leftTicketInit(Map<String, String> cookieMap) {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax get 获取订单页dynamicJs的内容，从中提取出订票key---");
        }
        
        HttpGet get = new HttpGet(UrlConstants.GET_LEFT_TICKET_INIT_URL);
        HttpHeader.setLoginInitHeader(get);
        setCookie(get, cookieMap);
        
        String leftTicketDynamicJs = null;
        try {
            String result = doGetRequest(get);
            
            Matcher m_token = PATTERN_DYNAMIC_JS.matcher(result);
            if (m_token.find()) {
                leftTicketDynamicJs = m_token.group(1);
            }
            else {
                logger.error("httpClient init get leftTicketDynamicJsUrl  fail for unknow reason, check it!");
            }
        }
        catch (Exception e) {
            logger.error("leftTicketInit error : ", e);
        }
        
        if (leftTicketDynamicJs != null) {
            orderTicketKey = getRandomParamKey(leftTicketDynamicJs);
        }
    }
    
    /**
     * 判断用户是否已登录
     * @return 是否登录
     */
    public boolean checkUserLogin() {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax post 检查是否登录---");
        }
        
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("_json_att", ""));
        
        HttpPost post = new HttpPost(UrlConstants.REQ_CHECK_USER_URL);
        HttpHeader.setPostAjaxHeader(post);
        setCookie(post, null);
        
        boolean result = false;
        try {
            String checkResult = doPostRequest(post, params);
            JsonMsg4Login msg = JSONObject.parseObject(checkResult, JsonMsg4Login.class);
            if (!msg.getStatus()) {
                result = false;
                System.out.println(msg.getMessages()[0]);
            }
            else {
                result = msg.getData().getBoolean("flag");
            }
        }
        catch (Exception e) {
            logger.error("检查是否登录发生异常", e);
            TicketMainFrame.remind("检查是否登录发生异常");
        }
        
        return result;
    }
    
    /**
     * <选择某个车次进入预订页前检验>
     * @param configInfo
     * @param cookieMap
     * @param train
     * @return
     */
    public String submitOrderRequest(TicketConfigInfo configInfo, Map<String, String> cookieMap, TrainInfo train) {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax post 进入预订页面前检验---");
        }
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        if (orderTicketKey != null) {
            params.add(new BasicNameValuePair(orderTicketKey, DynamicJsUtil.getRandomParamValue(orderTicketKey)));
        }
        params.add(new BasicNameValuePair("myversion", "undefined"));
        try {
            params.add(new BasicNameValuePair("secretStr", URLDecoder.decode(train.getSecretStr(), "UTF-8")));
        }
        catch (UnsupportedEncodingException e1) {
            logger.error("进入预订页参数编码错误", e1);
            TicketMainFrame.remind("预订页参数编码异常，请通知管理员!");
            return "预订页参数编码异常，请通知管理员!";
        }
        //        params.add(new BasicNameValuePair("secretStr", train.getSecretStr()));
        params.add(new BasicNameValuePair("train_date", configInfo.getTrain_date()));
        params.add(new BasicNameValuePair("back_train_date", DateUtils.formatDate(new Date())));
        params.add(new BasicNameValuePair("tour_flag", "dc"));//单程票标识
        params.add(new BasicNameValuePair("purpose_codes", configInfo.getPurpose_codes()));
        params.add(new BasicNameValuePair("query_from_station_name", configInfo.getFrom_station_name()));
        params.add(new BasicNameValuePair("query_to_station_name", configInfo.getTo_station_name()));
        params.add(new BasicNameValuePair("undefined", ""));
        
        HttpPost post = new HttpPost(UrlConstants.REQ_SUBMIT_ORDER_URL);
        HttpHeader.setPostAjaxHeader(post);
        setCookie(post, cookieMap);
        
        String result = null;
        try {
            String checkResult = doPostRequest(post, params);
            
            int reConnCount = 0;
            while (reConnCount < 3 && StringUtils.isEmpty(checkResult)) {
                System.out.println("----reconn-----");
                try {
                    Thread.sleep(100L);
                }
                catch (InterruptedException e) {
                }
                reConnCount++;
                checkResult = doPostRequest(post, params);
            }
            JsonMsg4SubmitOrder msg = JSONObject.parseObject(checkResult, JsonMsg4SubmitOrder.class);
            if (msg != null) {
                if (msg.getHttpstatus() == 200 && msg.getStatus()) {
                    
                }
                else {
                    if (msg.getMessages() != null) {
                        TicketMainFrame.remind(msg.getMessages()[0]);
                        result = msg.getMessages()[0];
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error("进入预订页面校验失败", e);
            TicketMainFrame.remind("进入预订页面校验失败");
            result = "进入预订页面校验失败";
        }
        
        return result;
    }
    
    /**
     * <选择某个车次进入预订页>
     * @param cookieMap
     * @return
     */
    public String getInitDcPage(Map<String, String> cookieMap) {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax post 进入单程票预订页---");
        }
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("_json_att", ""));
        
        HttpPost post = new HttpPost(UrlConstants.REQ_INITDC_URL);
        HttpHeader.setInitDcHeader(post);
        setCookie(post, cookieMap);
        
        String result = null;
        String checkOrderDynamicJs = null;
        try {
            result = doPostRequest(post, params);
            
            // 获取订单确认key
            Matcher m_token = PATTERN_DYNAMIC_JS.matcher(result);
            if (m_token.find()) {
                checkOrderDynamicJs = m_token.group(1);
            }
        }
        catch (Exception e) {
            logger.error("进入单程票预订页", e);
            TicketMainFrame.remind("进入单程票预订页");
        }
        
        if (checkOrderDynamicJs != null) {
            checkOrderKey = getRandomParamKey(checkOrderDynamicJs);
        }
        
        return result;
    }
    
    public File buildOrderCodeImage(Map<String, String> cookies) {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax get 获取提交订单验证码---");
            logger.debug("[get url] {}", UrlConstants.GET_ORDER_PASSCODE_URL);
        }
        
        return buildCodeImage(UrlConstants.GET_ORDER_PASSCODE_URL, ".order.png");
    }
    
    public JsonMsg4CheckOrder checkOrderAuthcode(String authCode, String token, List<PassengerData> passengers,
        Map<String, String> cookieMap) {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax post 检查下单验证码---");
        }
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("bed_level_order_num", "000000000000000000000000000000"));
        params.add(new BasicNameValuePair("cancel_flag", "2"));
        params.add(new BasicNameValuePair("passengerTicketStr", getPassengerTicketStr(passengers)));
        params.add(new BasicNameValuePair("oldPassengerStr", getOldPassengerStr(passengers)));
        params.add(new BasicNameValuePair("randCode", authCode));
        params.add(new BasicNameValuePair("REPEAT_SUBMIT_TOKEN", token));
        params.add(new BasicNameValuePair("tour_flag", "dc"));
        params.add(new BasicNameValuePair("_json_att", ""));
        if (checkOrderKey != null) {
            params.add(new BasicNameValuePair(checkOrderKey, DynamicJsUtil.getRandomParamValue(checkOrderKey)));
        }
        
        HttpPost post = new HttpPost(UrlConstants.REQ_CHECK_ORDER_URL);
        HttpHeader.setPostAjaxHeader(post);
        setCookie(post, cookieMap);
        
        JsonMsg4CheckOrder result = null;
        try {
            String checkResult = doPostRequest(post, params);
            int reConnCount = 0;
            while (reConnCount < 3 && StringUtils.isEmpty(checkResult)) {
                System.out.println("----reconn-----");
                try {
                    Thread.sleep(100L);
                }
                catch (InterruptedException e) {
                }
                reConnCount++;
                checkResult = doPostRequest(post, params);
            }
            result = JSONObject.parseObject(checkResult, JsonMsg4CheckOrder.class);
        }
        catch (Exception e) {
            logger.error("检查下单验证码发生异常", e);
            TicketMainFrame.remind("检查下单验证码发生异常,请联系管理员");
        }
        
        return result;
    }
    
    public JsonMsg4QueueCount getQueueCount(TrainInfo train, Date trianDate, String seatType, String token,
        Map<String, String> cookieMap) {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax post 获取排队人数---");
        }
        DateFormat df = new SimpleDateFormat("EEE MMM dd yyyy", Locale.US);
        
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("purpose_codes", "00"));
        params.add(new BasicNameValuePair("seatType", seatType));
        params.add(new BasicNameValuePair("train_no", train.getTrain_no()));
        params.add(new BasicNameValuePair("fromStationTelecode", train.getFrom_station_telecode()));
        params.add(new BasicNameValuePair("stationTrainCode", train.getStation_train_code()));
        params.add(new BasicNameValuePair("toStationTelecode", train.getTo_station_telecode()));
        params.add(new BasicNameValuePair("leftTicket", train.getYp_info()));
        params.add(new BasicNameValuePair("REPEAT_SUBMIT_TOKEN", token));
        params.add(new BasicNameValuePair("train_date", df.format(trianDate) + " 00:00:00 GMT+0800"));//Sun Feb 01 2015 00:00:00 GMT+0800
        params.add(new BasicNameValuePair("_json_att", ""));
        
        HttpPost post = new HttpPost(UrlConstants.REQ_QUEUE_COUNT_URL);
        HttpHeader.setPostAjaxHeader(post);
        setCookie(post, cookieMap);
        
        JsonMsg4QueueCount result = null;
        try {
            String checkResult = doPostRequest(post, params);
            int reConnCount = 0;
            while (reConnCount < 3 && StringUtils.isEmpty(checkResult)) {
                System.out.println("----reconn-----");
                try {
                    Thread.sleep(100L);
                }
                catch (InterruptedException e) {
                }
                reConnCount++;
                checkResult = doPostRequest(post, params);
            }
            result = JSONObject.parseObject(checkResult, JsonMsg4QueueCount.class);
        }
        catch (Exception e) {
            logger.error("获取排队人数发生异常", e);
            TicketMainFrame.remind("获取排队人数发生异常");
        }
        return result;
    }
    
    public JsonMsg4ConfirmQueue confirmQueue(TrainInfo train, OrderToken token, List<PassengerData> passengers,
        String authcode, Map<String, String> cookieMap) {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax post 确认排队购买---");
        }
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("purpose_codes", "00"));
        params.add(new BasicNameValuePair("key_check_isChange", token.getKey_check_isChange()));
        params.add(new BasicNameValuePair("passengerTicketStr", getPassengerTicketStr(passengers)));
        params.add(new BasicNameValuePair("oldPassengerStr", getOldPassengerStr(passengers)));
        params.add(new BasicNameValuePair("randCode", authcode));
        params.add(new BasicNameValuePair("leftTicketStr", train.getYp_info()));
        params.add(new BasicNameValuePair("REPEAT_SUBMIT_TOKEN", token.getToken()));
        params.add(new BasicNameValuePair("train_location", train.getLocation_code()));
        params.add(new BasicNameValuePair("_json_att", ""));
        
        HttpPost post = new HttpPost(UrlConstants.REQ_CONFIRM_SINGLE_URL);
        HttpHeader.setPostAjaxHeader(post);
        setCookie(post, cookieMap);
        
        JsonMsg4ConfirmQueue result = null;
        try {
            String checkResult = doPostRequest(post, params);
            int reConnCount = 0;
            while (reConnCount < 3 && StringUtils.isEmpty(checkResult)) {
                System.out.println("----reconn-----");
                try {
                    Thread.sleep(100L);
                }
                catch (InterruptedException e) {
                }
                reConnCount++;
                checkResult = doPostRequest(post, params);
            }
            result = JSONObject.parseObject(checkResult, JsonMsg4ConfirmQueue.class);
        }
        catch (Exception e) {
            logger.error("确认排队购买发生异常", e);
            TicketMainFrame.remind("确认排队购买发生异常");
        }
        return result;
    }
    
    /**
     * <查询余票信息>
     * @param configInfo
     * @param cookieMap
     * @return
     */
    public JsonMsg4QueryWait queryOrderWaitTime(String token, Map<String, String> cookieMap) {
        if (logger.isDebugEnabled()) {
            logger.debug("---ajax get 查询排队等待信息---");
        }
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("random", String.valueOf(System.currentTimeMillis())));
        params.add(new BasicNameValuePair("tourFlag", "dc"));
        params.add(new BasicNameValuePair("_json_att", ""));
        params.add(new BasicNameValuePair("REPEAT_SUBMIT_TOKEN", token));
        String paramsUrl = URLEncodedUtils.format(params, "UTF-8");
        
        HttpGet get = new HttpGet(UrlConstants.GET_QUERY_ORDER_WAIT_URL + "?" + paramsUrl);
        HttpHeader.setGetAjaxHeader(get);
        setCookie(get, cookieMap);
        
        JsonMsg4QueryWait result = null;
        try {
            String checkResult = doGetRequest(get);
            result = JSONObject.parseObject(checkResult, JsonMsg4QueryWait.class);
        }
        catch (Exception e) {
            logger.error("查询排队等待信息异常", e);
            TicketMainFrame.remind("查询排队等待信息异常");
        }
        return result;
    }
    
    /**
     * <退出>
     *
     */
    public void loginOut() {
        if (logger.isDebugEnabled()) {
            logger.debug("---post 登录退出---");
        }
        HttpGet get = new HttpGet(UrlConstants.GET_LOGOUT_URL);
        HttpHeader.setGetAjaxHeader(get);
        setCookie(get, null);
        
        try {
            doGetRequest(get);
        }
        catch (Exception e) {
            logger.error("退出发生异常", e);
        }
        TicketMainFrame.trace("退出成功!");
    }
    
    private static X509TrustManager tm = new X509TrustManager() {
        public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        }
        
        public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        }
        
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    };
    
    private static HttpClient buildHttpClient() {
        SSLContext sslcontext = null;
        try {
            sslcontext = SSLContext.getInstance("TLS");
        }
        catch (NoSuchAlgorithmException e) {
            logger.error("getHttpClient error for NoSuchAlgorithmException", e);
        }
        
        try {
            sslcontext.init(null, new TrustManager[]
            {tm}, null);
        }
        catch (KeyManagementException e) {
            logger.error("getHttpClient error for KeyManagementException", e);
        }
        SSLSocketFactory ssf = new SSLSocketFactory(sslcontext);
        ClientConnectionManager ccm = new DefaultHttpClient().getConnectionManager();
        SchemeRegistry sr = ccm.getSchemeRegistry();
        sr.register(new Scheme("https", 443, ssf));
        HttpParams params = new BasicHttpParams();
        params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);
        params.setParameter(CoreConnectionPNames.SO_TIMEOUT, 5000);
        HttpClient httpclient = new DefaultHttpClient(ccm, params);
        return httpclient;
    }
    
    /**
     * <公用POST请求方法>
     * 
     * @param http
     * @param params
     * @return
     */
    private String doPostRequest(HttpPost request, List<NameValuePair> params) {
        if (logger.isDebugEnabled()) {
            logger.debug("[post url] {}", request.getURI().toString());
        }
        HttpClient httpclient = buildHttpClient();
        String responseBody = null;
        
        InputStream is = null;
        try {
            if (params != null) {
                UrlEncodedFormEntity uef = new UrlEncodedFormEntity(params, "UTF-8");
                request.setEntity(uef);
            }
            if (logger.isDebugEnabled()) {
                if (params == null)
                    logger.debug("[post params] null");
                else {
//                    logger.debug("[post params] {}", URLEncodedUtils.format(params, "UTF-8"));
                    //不打印密码参数
                    if (!URLEncodedUtils.format(params, "UTF-8").contains("password")) {
                        logger.debug("[post params] {}", URLEncodedUtils.format(params, "UTF-8"));
                    }
                    else {
                        logger.debug("[post params] {包含密码不打日志，安全第一，呵呵}");
                    }
                }
            }
            HttpResponse response = httpclient.execute(request);
            Header[] headers = response.getAllHeaders();
            boolean isGzip = false;
            for (int i = 0; i < headers.length; i++) {
                if ("Content-Encoding".equals(headers[i].getName()) && "gzip".equals(headers[i].getValue())) {
                    isGzip = true;
                    break;
                }
            }
            is = response.getEntity().getContent();
            if (isGzip) {
                responseBody = zipInputStream(is);
            }
            else {
                responseBody = readInputStream(is);
            }
            
        }
        catch (Exception e) {
            logger.error("doPostRequest error:", e);
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                }
            }
            httpclient.getConnectionManager().shutdown();
        }
        
        if (logger.isDebugEnabled()) {
            if (responseBody != null && responseBody.length() < DEBBUG_MAX_COUNT)
                logger.debug("[responseBody] {}", responseBody);
        }
        return responseBody;
    }
    
    /**
     * <公用get请求>
     * @param request
     * @return
     */
    private String doGetRequest(HttpGet request) {
        if (logger.isDebugEnabled()) {
            logger.debug("[get url] {}", request.getURI().toString());
        }
        HttpClient httpclient = buildHttpClient();
        String responseBody = null;
        
        InputStream is = null;
        try {
            HttpResponse response = httpclient.execute(request);
            Header[] headers = response.getAllHeaders();
            boolean isGzip = false;
            for (int i = 0; i < headers.length; i++) {
                if ("Content-Encoding".equals(headers[i].getName()) && "gzip".equals(headers[i].getValue())) {
                    isGzip = true;
                    break;
                }
            }
            is = response.getEntity().getContent();
            if (isGzip) {
                responseBody = zipInputStream(is);
            }
            else {
                responseBody = readInputStream(is);
            }
            
        }
        catch (Exception e) {
            logger.error("doGetRequest error:", e);
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                }
            }
            httpclient.getConnectionManager().shutdown();
        }
        if (logger.isDebugEnabled()) {
            if (responseBody != null && responseBody.length() < DEBBUG_MAX_COUNT)
                logger.debug("[responseBody] {}", responseBody);
        }
        return responseBody;
    }
    
    /**
     * 公有获取请求返回头
     * @param request
     * @return
     */
    private Header[] doGetHeaderRequest(HttpGet request) {
        if (logger.isDebugEnabled()) {
            logger.debug("[get url] {}", request.getURI().toString());
        }
        HttpClient httpclient = buildHttpClient();
        
        Header[] headers = null;
        try {
            HttpResponse response = httpclient.execute(request);
            
            headers = response.getAllHeaders();   
        }
        catch (Exception e) {
            logger.error("doGetHeaderRequest error:", e);
        }
        finally {
            httpclient.getConnectionManager().shutdown();
        }

        return headers;
    }
    
    private void setCookie(HttpRequestBase request, Map<String, String> cookieMap) {
        request.setHeader("Cookie", getCookieStr(cookieMap));
    }
    
    private String getCookieStr(Map<String, String> cookieMap) {
        String cookie;
        if (cookieMap != null && cookieMap.size() > 0) {
            cookie = "JSESSIONID=" + JSESSIONID;
            for (Map.Entry<String, String> entry : cookieMap.entrySet()) {
                cookie += "; " + entry.getKey() + "=" + entry.getValue();
            }
            cookie += ";BIGipServerotn=" + BIGipServerotn;
            if (current_captcha_type != null) {
                cookie += ";current_captcha_type=" + current_captcha_type;
            }
        }
        else {
            cookie = "JSESSIONID=" + JSESSIONID + ";BIGipServerotn=" + BIGipServerotn;
            if (current_captcha_type != null) {
                cookie += ";current_captcha_type=" + current_captcha_type;
            }
        }
        logger.debug("[request cooikes] {}", cookie);
        return cookie;
    }
    
    /**
     * 获取参数oldPassengerStr值
     * 
     * @param passengers
     * @return
     */
    private String getOldPassengerStr(List<PassengerData> passengers) {
        StringBuilder sb = new StringBuilder();
        for (PassengerData p : passengers) {
            sb.append(p.getName())
                .append(',')
                .append(p.getCardTypeValue())
                .append(",")
                .append(p.getCardNo())
                .append(',')
                .append(p.getTicketTypeValue())
                .append('_');
        }
        return sb.toString();
    }
    
    /**
     * 获取oldPassengerStr
     * 
     * @param userInfo
     * @return
     */
    private String getPassengerTicketStr(List<PassengerData> passengers) {
        StringBuilder sb = new StringBuilder();
        for (PassengerData p : passengers) {
            if (p.getSeatType() != SeatType.NONE_SEAT) {
                sb.append(p.getSeatTypeValue());
            }
            sb.append(",0,")
                .append(p.getTicketTypeValue())
                .append(',')
                .append(p.getName())
                .append(',')
                .append(p.getCardTypeValue())
                .append(",")
                .append(p.getCardNo())
                .append(',')
                .append(p.getMobileNotNull())
                .append(",N_");
        }
        
        return sb.substring(0, sb.length() - 1);
    }
    
    /**
     * 处理返回文件流
     * 
     * @param is
     * @return
     * @throws IOException
     */
    private static String readInputStream(InputStream is) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuffer buffer = new StringBuffer();
        String line;
        while ((line = in.readLine()) != null)
            buffer.append(line + "\n");
        return buffer.toString();
    }
    
    /**
     * 处理gzip,deflate返回流
     * 
     * @param is
     * @return
     * @throws IOException
     */
    private static String zipInputStream(InputStream is) throws IOException {
        GZIPInputStream gzip = new GZIPInputStream(is);
        BufferedReader in = new BufferedReader(new InputStreamReader(gzip, "UTF-8"));
        StringBuffer buffer = new StringBuffer();
        String line;
        while ((line = in.readLine()) != null)
            buffer.append(line + "\n");
        return buffer.toString();
    }
}
