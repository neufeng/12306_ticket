package com.free.app.ticket.view;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import java.util.List;

import com.free.app.ticket.model.TrainInfo;

public class SelTrainTableModel extends AbstractTableModel {
    
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 5066202337432757506L;
    
    // 表头
    String[] head = { "选择", "车次", "出发站", "到达站", "出发时间", "到达时间", "历时", "一等座", "二等座", "软卧", "硬卧", "软座", "硬座", "无座" };
    
    // 类型
    Class[] typeArray = { Boolean.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class };
    
    // 数据
    private List<TrainInfo> trainData = null;
    // 是否选择
    private boolean[] selIndexes = null;
    
    public SelTrainTableModel() {

    }
    
    public SelTrainTableModel(List<TrainInfo> trainData) {
        this.trainData = trainData;
        this.selIndexes = new boolean[trainData.size()];
    }

    @Override
    public int getRowCount() {
        // TODO Auto-generated method stub
        if (trainData == null) {
            return 0;
        }
        
        return trainData.size();
    }
    
    @Override
    public int getColumnCount() {
        // TODO Auto-generated method stub
        return head.length;
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        // TODO Auto-generated method stub
        if (trainData == null || rowIndex >= trainData.size()) {
            return null;
        }
        
        Object value = null;
        TrainInfo trainInfo = trainData.get(rowIndex);
        switch (columnIndex) {
            case 0: //选择
                value = new Boolean(rowIndex < selIndexes.length ? selIndexes[rowIndex] : false);
                break;
            case 1: //车次
                value = trainInfo.getStation_train_code();
                break;
            case 2: //出发站
                value = trainInfo.getFrom_station_name();
                break;
            case 3: //到达站
                value = trainInfo.getTo_station_name();
                break;
            case 4: //出发时间
                value = trainInfo.getStart_time();
                break;
            case 5: //到达时间
                value = trainInfo.getArrive_time();
                break;
            case 6: //历时
                value = trainInfo.getLishi();
                break;
            case 7: //一等座
                value = trainInfo.getZy_num();
                break;
            case 8: //二等座
                value = trainInfo.getZe_num();
                break;
            case 9: //软卧
                value = trainInfo.getRw_num();
                break;
            case 10: //硬卧
                value = trainInfo.getYw_num();
                break;
            case 11: //软座
                value = trainInfo.getRz_num();
                break;
            case 12: //硬座
                value = trainInfo.getYz_num();
                break;
            case 13: //无座
                value = trainInfo.getWz_num();
                break;
        }
        
        return value;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        // TODO Auto-generated method stub
        if (columnIndex == 0) {
            Boolean boolValue = (Boolean)aValue;
            boolean isChecked = boolValue.booleanValue();
            if ((isChecked && getCheckedCount() < 5) || !isChecked) {
                selIndexes[rowIndex] = isChecked;
            }
            fireTableChanged(new TableModelEvent(this, rowIndex, rowIndex, 0, TableModelEvent.UPDATE));
        }
        
        super.setValueAt(aValue, rowIndex, columnIndex);
    }

    @Override
    public String getColumnName(int column) {
        // TODO Auto-generated method stub
        return head[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        // TODO Auto-generated method stub
        return typeArray[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // TODO Auto-generated method stub
        if (columnIndex == 0) {
            return true;
        }
        
        return super.isCellEditable(rowIndex, columnIndex);
    }

    public boolean[] getSelIndexes() {
        return selIndexes;
    }

    public void setTrainData(List<TrainInfo> trainData) {
        if (trainData == null) {
            return;
        }
        
        this.trainData = trainData;
        this.selIndexes = new boolean[trainData.size()];
    }
    
    private int getCheckedCount() {
        int count = 0;
        for (int i = 0; i < selIndexes.length; i++)  {
            if (selIndexes[i]) {
                count++;
            }
        }
        
        return count;
    }
    
}
