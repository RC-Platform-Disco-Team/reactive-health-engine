/*
 * Created by JFormDesigner on Mon Aug 28 18:19:56 MSK 2017
 */

package com.ringcentral.platform.health.demo;

import com.ringcentral.platform.health.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("FieldCanBeLocal")
public class UI extends JFrame {

    private List<HealthCheckFunction> functions;
    private Runnable forceSync, forceAsync;
    private Consumer<Object> sendPassiveOk, sendPassiveFail;

    public UI(List<HealthCheckFunction> func, Runnable forceSync, Runnable forceAsync,
              Consumer<Object> sendPassiveOk, Consumer<Object> sendPassiveFail) {
        this.functions = func;
        this.forceSync = forceSync;
        this.forceAsync = forceAsync;
        this.sendPassiveOk = sendPassiveOk;
        this.sendPassiveFail = sendPassiveFail;
        initComponents();
    }

    private void initComponents() {
        globalLabel = new JLabel();
        global = new JTextField();
        global.setFocusable(false);
        lastChangedLabel = new JLabel();
        lastChanged = new JTextField();
        lastChanged.setFocusable(false);

        forceSyncButton = new JButton();
        forceSyncButton.setText("Force Sync");
        forceSyncButton.addActionListener(this::forceSyncButtonActionPerformed);

        forceAsyncButton = new JButton();
        forceAsyncButton.setText("Force Async");
        forceAsyncButton.addActionListener(this::forceAsyncButtonActionPerformed);

        TableModel dataModel = new DefaultTableModel(functions.size(), 5);
        table = new JTable(dataModel);

        table.setFillsViewportHeight(true);

        table.getColumnModel().getColumn(0).setMaxWidth(100);
        table.getColumnModel().getColumn(1).setMaxWidth(180);
        table.getColumnModel().getColumn(2).setMaxWidth(50);
        table.getColumnModel().getColumn(3).setMaxWidth(45);
        table.getColumnModel().getColumn(4).setMaxWidth(45);

        table.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer("+"));
        table.getColumnModel().getColumn(3).setCellEditor(new ButtonEditor("+", sendPassiveOk));

        table.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer("-"));
        table.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor("-", sendPassiveFail));

        Container contentPane = getContentPane();

        globalLabel.setText("Global State");

        lastChangedLabel.setText("Last Changed");

        GroupLayout contentPaneLayout = new GroupLayout(contentPane);
        contentPane.setLayout(contentPaneLayout);
        contentPaneLayout.setHorizontalGroup(
                contentPaneLayout.createParallelGroup()
                        .addGroup(contentPaneLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(table, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                .addGroup(contentPaneLayout.createSequentialGroup()
                                                        .addComponent(globalLabel)
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(global, GroupLayout.PREFERRED_SIZE, 289, GroupLayout.PREFERRED_SIZE))
                                                .addGroup(contentPaneLayout.createSequentialGroup()
                                                        .addComponent(lastChangedLabel)
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                        .addComponent(lastChanged, GroupLayout.PREFERRED_SIZE, 289, GroupLayout.PREFERRED_SIZE))
                                                .addGroup(contentPaneLayout.createSequentialGroup()
                                                        .addComponent(forceSyncButton, GroupLayout.PREFERRED_SIZE, 190, GroupLayout.PREFERRED_SIZE)
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(forceAsyncButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        contentPaneLayout.setVerticalGroup(
                contentPaneLayout.createParallelGroup()
                        .addGroup(contentPaneLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(globalLabel)
                                        .addComponent(global, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addGap(12, 12, 12)
                                .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lastChangedLabel)
                                        .addComponent(lastChanged, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(table, GroupLayout.DEFAULT_SIZE, 149, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(forceSyncButton)
                                        .addComponent(forceAsyncButton))
                                .addContainerGap())
        );
        pack();
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private JLabel globalLabel;
    private JTextField global;
    private JLabel lastChangedLabel;
    private JTextField lastChanged;
    private JTable table;
    private JButton forceSyncButton;
    private JButton forceAsyncButton;

    private void setValue(HealthStateEnum globalState, Instant lc, Map<HealthCheckID, LatestHealthCheckState> details) {
        global.setText(globalState.name());
        lastChanged.setText(TimeFormatter.format(lc));
        int i = 0;
        for (HealthCheckID id : details.keySet()) {
            table.getModel().setValueAt(id.getShortName(), i, 0);
            table.getModel().setValueAt(TimeFormatter.format(details.get(id).getLastChanged()), i, 1);
            table.getModel().setValueAt(details.get(id).getState(), i, 2);
            table.getModel().setValueAt(id, i, 3);
            table.getModel().setValueAt(id, i, 4);
            i++;
        }
    }

    void update(HealthStateEnum globalState, Instant lc, Map<HealthCheckID, LatestHealthCheckState> details) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        SwingUtilities.invokeLater(() -> this.setValue(globalState, lc, details));
    }

    private void forceSyncButtonActionPerformed(ActionEvent ignored) {
        forceSync.run();
    }

    private void forceAsyncButtonActionPerformed(ActionEvent ignored) {
        forceAsync.run();
    }

    class ButtonRenderer extends JButton implements TableCellRenderer {

        private String label;

        ButtonRenderer(String label) {
            setOpaque(true);
            this.label = label;
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(UIManager.getColor("Button.background"));
            }
            setText(label);
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private Object value;
        private boolean isPushed;
        private Consumer<Object> action;

        ButtonEditor(String text, Consumer<Object> action) {
            super(new JCheckBox());
            this.action = action;
            button = new JButton();
            button.setOpaque(true);
            button.setText(text != null ? text : "");
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            if (isSelected) {
                button.setForeground(table.getSelectionForeground());
                button.setBackground(table.getSelectionBackground());
            } else {
                button.setForeground(table.getForeground());
                button.setBackground(table.getBackground());
            }
            this.value = value;
            isPushed = true;
            return button;
        }

        public Object getCellEditorValue() {
            if (isPushed) {
                action.accept(value);
            }
            isPushed = false;
            return value;
        }

        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
}
