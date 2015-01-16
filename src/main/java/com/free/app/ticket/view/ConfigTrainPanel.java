package com.free.app.ticket.view;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.free.app.ticket.model.TrainConfigInfo;
import com.free.app.ticket.model.TrainData4UI.UserTrainInfo;

public class ConfigTrainPanel extends JPanel {
    
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 8712901964335697205L;

    private static List<JCheckBox> boxes = null;
    
    private static List<UserTrainInfo> selTrainInfo = null;
    
    TrainItemListener itemListener = null;
    
    public ConfigTrainPanel() {
        this.setBounds(10, 65, 770, 34);
        
        JLabel trainLabel = new JLabel("已选车次");
        trainLabel.setBounds(10, 26, 40, 17);
        this.add(trainLabel);
        
        selTrainInfo = new ArrayList<UserTrainInfo>();
        
        boxes = new ArrayList<JCheckBox>();
        
        itemListener = new TrainItemListener();
    }
    
    /**
     * 增加抢票车次
     * @param trainInfo
     */
    public void addTrain(UserTrainInfo trainInfo) {
        // 最多选择5个车次
        if (selTrainInfo.contains(trainInfo) || selTrainInfo.size() >= 5) {
            return;
        }
        
        selTrainInfo.add(trainInfo);
        
        JCheckBox cb = new JCheckBox(trainInfo.getTrainCode(), true);
        cb.setBounds(10, 7, 60, 20);
        boxes.add(cb);
        this.add(cb);
        
        cb.addItemListener(itemListener);
        
        this.revalidate();
        this.repaint();
    }
    
    /**
     * 情况抢票车次
     */
    public void clearTrains() {
        for (int i = 0; i < boxes.size(); i++) {
            JCheckBox cb = boxes.get(i);
            this.remove(cb);
        }
        boxes.clear();
        selTrainInfo.clear();
    }
    
    /**
     * 获取抢票车次配置
     * @return
     */
    public TrainConfigInfo getTrainConfigInfo() {
        List<UserTrainInfo> userTrains = new ArrayList<UserTrainInfo>();
        
        for (int i = 0; i < boxes.size(); i++) {
            JCheckBox cb = boxes.get(i);
            if (cb.isSelected()) {
                UserTrainInfo trainInfo = selTrainInfo.get(i);
                userTrains.add(trainInfo);
            }
        }
        
        if (userTrains.size() <= 0) {
            return null;
        }

        TrainConfigInfo trainConfigInfo = new TrainConfigInfo(userTrains);
        
        return trainConfigInfo;
    }
    
    class TrainItemListener implements ItemListener{

        @Override
        public void itemStateChanged(ItemEvent e) {
            JCheckBox cb = (JCheckBox)e.getItem();
            if (!cb.isSelected()) {
                int index = boxes.indexOf(cb);
                if (index != -1) {
                    boxes.remove(index);
                    selTrainInfo.remove(index);
                }
                JPanel panel = (JPanel)cb.getParent();
                panel.remove(cb);
                panel.revalidate();
                panel.repaint();
            }
        }
        
    }
    
}
