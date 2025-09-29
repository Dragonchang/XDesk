package com.xd.xdmanager.config;

public class Desk {
    //mqtt对应的clintid列如：x1y2
    private String clientId = "";
    //通过can控制主控的时候选择课桌的id
    private int canSubID;
    private int row;    // 行号
    private int column; // 列号
    private  boolean isAvailable = false;

    /**
     * 获取行号
     * @return 行号
     */
    public int getRow() {
        return row;
    }

    /**
     * 设置行号
     * @param row 新的行号
     */
    public void setRow(int row) {
        this.row = row;
    }

    /**
     * 获取列号
     * @return 列号
     */
    public int getColumn() {
        return column;
    }

    /**
     * 设置列号
     * @param column 新的列号
     */
    public void setColumn(int column) {
        this.column = column;
    }

    /**
     * 获取是否可用
     * @return 列号
     */
    public boolean getAvailable() {
        return isAvailable;
    }

    /**
     * 设置是否可用
     * @param available 新的列号
     */
    public void setAvailable(boolean available) {
        this.isAvailable = available;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Desk that = (Desk) o;
        return row == that.row && column == that.column;
    }


    @Override
    public String toString() {
        return "Desk{" +
                "clientId=" + clientId +
                "row=" + row +
                ", column=" + column +
                '}';
    }
}
