package Extension.Autorize;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

class ColorChangingRenderer extends DefaultTableCellRenderer {
    private AutorizeTableModel model;

    public ColorChangingRenderer(AutorizeTableModel model) {
        this.model = model;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        LogEntry logEntry = model.get(row);
        if (logEntry.authBypassResponseLen != logEntry.originalResponseLen || logEntry.unauthResponseLen != logEntry.originalResponseLen) {
            c.setBackground(Color.RED); // 当条件满足时设置背景为红色
        } else {
            c.setBackground(table.getBackground()); // 当条件不满足时使用默认背景色
        }
        return c;
    }
}
