package com.free.app.ticket.view;

import java.awt.Component;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.free.app.ticket.TicketMainFrame;
import com.free.app.ticket.model.TicketConfigInfo;
import com.free.app.ticket.model.TrainInfo;
import com.free.app.ticket.service.HttpClientThreadService;
import com.free.app.ticket.util.DateUtils;
import com.free.app.ticket.util.TicketHttpClient;

import javax.swing.ScrollPaneConstants;

public class SelTrainPanel extends JPanel {
    
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -7087355293585993598L;
    private JTable table;
    private SelTrainTableModel tableModel;
    
    private List<TrainInfo> trainData = null;
    
    private boolean isStop = false;
    
    /**
     * Create the panel.
     */
    public SelTrainPanel() {
        this.setBorder(new TitledBorder("选择车次"));
        
        tableModel = new SelTrainTableModel();
        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tableModel.addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                // TODO Auto-generated method stub
                if (e.getColumn() == 0 && e.getFirstRow() == e.getLastRow()) {
                    return;
                }
                
                sizeColumnsToFit(table, 5);
            }
            
        });
        
        JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setBounds(10, 20, 360, 260);
        this.add(scrollPane);
        
        this.addAncestorListener(new AncestorListener() {

            @Override
            public void ancestorAdded(AncestorEvent event) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                // TODO Auto-generated method stub
                isStop = true;
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
                // TODO Auto-generated method stub
                
            }
            
        });
        
        initTrainData();
    }
    
    public List<TrainInfo> getSelectTrains() {
        boolean selIndexes[] = tableModel.getSelIndexes();
        List<TrainInfo> trains = new ArrayList<TrainInfo>();
        for (int i = 0; i < selIndexes.length && trains.size() < 5; i++) {
            if (selIndexes[i]) {
                trains.add(trainData.get(i));
            }
        }
        
        return trains;
    }
    
    public SelTrainTableModel getTableModel() {
        return tableModel;
    }

    public void setTableModel(SelTrainTableModel tableModel) {
        this.tableModel = tableModel;
    }

    private void initTrainData() {
        TicketConfigInfo ticketConfigInfo = ConfigPanelManager.getTicketConfigInfo();
        if (ticketConfigInfo == null || ticketConfigInfo.getTrainDateAlias() == null) {
            TicketMainFrame.remind("还未配置好查询条件");
            return;
        }
        
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                while (!isStop) {
                    if (queryTicket()) {
                        TicketMainFrame.trace("查询余票成功");
                        break;
                    }
                    
                    TicketMainFrame.trace("查询不到余票信息,3秒后开始下一轮查询");
                    try {
                        Thread.sleep(3000L);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                if (isStop) {
                    return;
                }
                
                tableModel.setTrainData(trainData);
                tableModel.fireTableDataChanged();
            }
            
        });
        t.start();
    }
    
    /**
     * 自动调整列宽
     * @param table
     * @param columnMargin
     */
    private void sizeColumnsToFit(JTable table, int columnMargin) {
        JTableHeader tableHeader = table.getTableHeader();
 
        if(tableHeader == null) {
            // can't auto size a table without a header
            return;
        }
 
        FontMetrics headerFontMetrics = tableHeader.getFontMetrics(tableHeader.getFont());
 
        int[] minWidths = new int[table.getColumnCount()];
        int[] maxWidths = new int[table.getColumnCount()];
 
        for(int columnIndex = 0; columnIndex < table.getColumnCount(); columnIndex++) {
            int headerWidth = headerFontMetrics.stringWidth(table.getColumnName(columnIndex));
 
            minWidths[columnIndex] = headerWidth + columnMargin;
 
            int maxWidth = getMaximalRequiredColumnWidth(table, columnIndex, headerWidth);
 
            maxWidths[columnIndex] = Math.max(maxWidth, minWidths[columnIndex]) + columnMargin;
        }
 
        adjustMaximumWidths(table, minWidths, maxWidths);
 
        for(int i = 0; i < minWidths.length; i++) {
            if(minWidths[i] > 0) {
                table.getColumnModel().getColumn(i).setMinWidth(minWidths[i]);
            }
 
            if(maxWidths[i] > 0) {
                table.getColumnModel().getColumn(i).setMaxWidth(maxWidths[i]);
 
                table.getColumnModel().getColumn(i).setWidth(maxWidths[i]);
            }
        }
    }
 
    private void adjustMaximumWidths(JTable table, int[] minWidths, int[] maxWidths) {
        if(table.getWidth() > 0) {
            // to prevent infinite loops in exceptional situations
            int breaker = 0;
 
            // keep stealing one pixel of the maximum width of the highest column until we can fit in the width of the table
            while(sum(maxWidths) > table.getWidth() && breaker < 10000) {
                int highestWidthIndex = findLargestIndex(maxWidths);
 
                maxWidths[highestWidthIndex] -= 1;
 
                maxWidths[highestWidthIndex] = Math.max(maxWidths[highestWidthIndex], minWidths[highestWidthIndex]);
 
                breaker++;
            }
        }
    }
 
    private int getMaximalRequiredColumnWidth(JTable table, int columnIndex, int headerWidth) {
        int maxWidth = headerWidth;
 
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
 
        TableCellRenderer cellRenderer = column.getCellRenderer();
 
        if(cellRenderer == null) {
            cellRenderer = new DefaultTableCellRenderer();
        }
 
        for(int row = 0; row < table.getModel().getRowCount(); row++) {
            Component rendererComponent = cellRenderer.getTableCellRendererComponent(table,
                table.getModel().getValueAt(row, columnIndex),
                false,
                false,
                row,
                columnIndex);
 
            double valueWidth = rendererComponent.getPreferredSize().getWidth();
 
            maxWidth = (int) Math.max(maxWidth, valueWidth);
        }
 
        return maxWidth;
    }
 
    private int findLargestIndex(int[] widths) {
        int largestIndex = 0;
        int largestValue = 0;
 
        for(int i = 0; i < widths.length; i++) {
            if(widths[i] > largestValue) {
                largestIndex = i;
                largestValue = widths[i];
            }
        }
 
        return largestIndex;
    }
 
    private int sum(int[] widths) {
        int sum = 0;
 
        for(int width : widths) {
            sum += width;
        }
 
        return sum;
    }
    
    /**
     * 查询余票
     * @return
     */
    private boolean queryTicket() {
        TicketConfigInfo ticketConfigInfo = ConfigPanelManager.getTicketConfigInfo();
        if (ticketConfigInfo == null || ticketConfigInfo.getTrainDateAlias() == null) {
            TicketMainFrame.remind("还未配置好查询条件");
            return false;
        }
        
        TicketMainFrame.trace("查询余票中...");
        TicketHttpClient client = HttpClientThreadService.getHttpClient();
        List<TrainInfo> trainInfos = client.queryLeftTicket(ticketConfigInfo, getCookie(ticketConfigInfo));
        
        if (trainInfos == null || trainInfos.isEmpty()) {
            return false;
        }
        
        this.trainData = trainInfos;
        return true;
    }
    
    private Map<String, String> getCookie(TicketConfigInfo config) {
        Map<String, String> cookies = new HashMap<String, String>();
        cookies.put("_jc_save_fromStation", getUnicode4Cookie(config.getFrom_station_name(), config.getFrom_station()));
        cookies.put("_jc_save_toStation", getUnicode4Cookie(config.getTo_station_name(), config.getTo_station()));
        cookies.put("_jc_save_fromDate", config.getTrain_date());
        cookies.put("_jc_save_toDate", DateUtils.formatDate(new Date()));
        cookies.put("_jc_save_wfdc_flag", "dc");
        cookies.put("_jc_save_showZtkyts", "true");
        return cookies;
    }
    
    public static String getUnicode4Cookie(String cityName, String cityCode) {
        String result = "";
        for (int i = 0; i < cityName.length(); i++) {
            int chr1 = (char)cityName.charAt(i);
            if (chr1 >= 19968 && chr1 <= 171941) {// 汉字范围 \u4e00-\u9fa5 (中文)
                result += "%u" + Integer.toHexString(chr1).toUpperCase();
            }
            else {
                result += cityName.charAt(i);
            }
        }
        result += "%2C" + cityCode;
        return result;
    }
    
}
