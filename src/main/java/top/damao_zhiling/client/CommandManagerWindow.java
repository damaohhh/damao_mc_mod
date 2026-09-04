package top.damao_zhiling.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import top.damao_zhiling.DamaoZlMod;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CommandManagerWindow extends JFrame {

    public static class CommandEntry {
        String name;
        String command;
        public CommandEntry(String name, String command) {
            this.name = name;
            this.command = command;
        }
    }

    private final List<CommandEntry> commands = new ArrayList<>();
    private JTable commandTable;
    private DefaultTableModel tableModel;
    private JLabel permissionLabel;

    private static final Path CONFIG_PATH = Paths.get("config", "damao_commands.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public CommandManagerWindow() {
        setTitle("Damao Mod - 命令管理器");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        loadCommands();

        tableModel = new DefaultTableModel(new String[]{"名称", "命令内容"}, 0);
        refreshTable();

        commandTable = new JTable(tableModel);
        commandTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        commandTable.getColumnModel().getColumn(1).setPreferredWidth(400);
        add(new JScrollPane(commandTable), BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(createAddButton());
        controlPanel.add(createDeleteButton());
        controlPanel.add(createApplyButton());
        controlPanel.add(createSaveButton());
        permissionLabel = new JLabel();
        updatePermissionStatus();
        controlPanel.add(permissionLabel);
        add(controlPanel, BorderLayout.SOUTH);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                saveCommands();
            }
        });
    }

    private JButton createAddButton() {
        JButton btn = new JButton("添加指令");
        btn.addActionListener(e -> addCommand());
        return btn;
    }

    private JButton createDeleteButton() {
        JButton btn = new JButton("删除选中指令");
        btn.addActionListener(e -> deleteSelected());
        return btn;
    }

    private JButton createApplyButton() {
        JButton btn = new JButton("应用到游戏");
        btn.addActionListener(e -> applyCommands());
        return btn;
    }

    private JButton createSaveButton() {
        JButton btn = new JButton("保存配置");
        btn.addActionListener(e -> saveCommands());
        return btn;
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (CommandEntry entry : commands) {
            tableModel.addRow(new Object[]{entry.name, entry.command});
        }
    }

    private void syncTableToCommands() {
        commands.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String name = (String) tableModel.getValueAt(i, 0);
            String cmd = (String) tableModel.getValueAt(i, 1);
            if (name != null && !name.trim().isEmpty() && cmd != null && !cmd.trim().isEmpty()) {
                commands.add(new CommandEntry(name.trim(), cmd.trim()));
            }
        }
    }

    private void addCommand() {
        String name = JOptionPane.showInputDialog(this, "指令名称（如：获得钻石）:");
        if (name == null || name.isBlank()) return;
        String cmd = JOptionPane.showInputDialog(this, "Minecraft 命令（以 / 开头）:\n例如 /give @p diamond 64");
        if (cmd == null || cmd.isBlank()) return;
        if (!cmd.startsWith("/")) {
            JOptionPane.showMessageDialog(this, "命令必须以 / 开头", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        commands.add(new CommandEntry(name.trim(), cmd.trim()));
        refreshTable();
        saveCommands();
    }

    private void deleteSelected() {
        int row = commandTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "请先选中要删除的行", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        commands.remove(row);
        refreshTable();
        saveCommands();
    }

    private void applyCommands() {
        syncTableToCommands();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            JOptionPane.showMessageDialog(this, "游戏内玩家未就绪", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!client.player.hasPermissionLevel(2)) {
            JOptionPane.showMessageDialog(this,
                    "当前玩家没有命令权限！\n请确保你是 OP 或在单人游戏中开启作弊。",
                    "权限不足", JOptionPane.WARNING_MESSAGE);
            updatePermissionStatus();
            return;
        }
        for (CommandEntry entry : commands) {
            String cmd = entry.command.startsWith("/") ? entry.command.substring(1) : entry.command;
            client.player.sendCommand(cmd);
            DamaoZlMod.LOGGER.info("Executed: {}", entry.command);
        }
        JOptionPane.showMessageDialog(this, "已应用 " + commands.size() + " 条指令", "完成", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveCommands() {
        syncTableToCommands();
        try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(commands, w);
            DamaoZlMod.LOGGER.info("Saved {} commands", commands.size());
        } catch (IOException e) {
            DamaoZlMod.LOGGER.error("Save failed", e);
            JOptionPane.showMessageDialog(this, "保存失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadCommands() {
        if (!Files.exists(CONFIG_PATH)) {
            commands.add(new CommandEntry("获得钻石", "/give @p diamond 1"));
            commands.add(new CommandEntry("晴天", "/weather clear"));
            saveCommands();
            return;
        }
        try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
            // 关键：使用完全限定名 java.lang.reflect.Type 避免冲突
            java.lang.reflect.Type type = new TypeToken<List<CommandEntry>>(){}.getType();
            List<CommandEntry> loaded = GSON.fromJson(r, type);
            if (loaded != null) {
                commands.clear();
                commands.addAll(loaded);
            }
        } catch (IOException e) {
            DamaoZlMod.LOGGER.error("Load failed", e);
        }
    }

    private void updatePermissionStatus() {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean hasPerm = client.player != null && client.player.hasPermissionLevel(2);
        if (hasPerm) {
            permissionLabel.setText("✓ 拥有命令权限");
            permissionLabel.setForeground(Color.GREEN);
        } else {
            permissionLabel.setText("✗ 无命令权限（需 OP/作弊）");
            permissionLabel.setForeground(Color.RED);
        }
    }
}