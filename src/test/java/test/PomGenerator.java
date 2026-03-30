package test;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * POM Scaffolder
 * Purpose: Automatically generate Selenium Page Object classes from a URL.
 */
public class PomGenerator extends JFrame {

    private static final long serialVersionUID = 1L;

    private final PomElementAnalyzer analyzer = new PomElementAnalyzer();
    private final PomCodeRenderer renderer = new PomCodeRenderer();

    private JTextField urlField;
    private JTextArea codeArea;
    private String generatedClassName = "GeneratedPage";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PomGenerator().setVisible(true));
    }

    public PomGenerator() {
        setTitle("POM Scaffolder");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder(" Target Configuration "));

        urlField = new JTextField("https://google.com");
        JButton generateBtn = new JButton("Generate Code");
        generateBtn.setPreferredSize(new Dimension(150, 30));

        topPanel.add(new JLabel(" Target URL or HTML file: "), BorderLayout.WEST);
        topPanel.add(urlField, BorderLayout.CENTER);
        topPanel.add(generateBtn, BorderLayout.EAST);

        codeArea = new JTextArea();
        codeArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        codeArea.setEditable(false);
        codeArea.setForeground(Color.BLACK);
        JScrollPane scrollPane = new JScrollPane(codeArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(" Generated POM "));

        JButton saveBtn = new JButton("Export to .java File");
        saveBtn.setFont(new Font("Arial", Font.BOLD, 14));
        saveBtn.setBackground(new Color(39, 174, 96));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setPreferredSize(new Dimension(0, 50));

        generateBtn.addActionListener(e -> processUrl());
        saveBtn.addActionListener(e -> saveToFile());

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(saveBtn, BorderLayout.SOUTH);
    }

    private void processUrl() {
        try {
            Document doc = loadDocument(urlField.getText().trim());
            generatedClassName = buildClassName(doc.title());
            codeArea.setText(renderer.render(generatedClassName, analyzer.analyze(doc)));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private Document loadDocument(String url) throws Exception {
        if (url.startsWith("file://")) {
            File file = new File(new java.net.URI(url));
            return Jsoup.parse(file, "UTF-8");
        }

        if (url.startsWith("http://") || url.startsWith("https://")) {
            return Jsoup.connect(url).get();
        }

        throw new Exception("Only http, https, and file:// protocols are supported");
    }

    private String buildClassName(String title) {
        String cleanedTitle = title == null ? "" : title.replaceAll("[^a-zA-Z0-9]", "");
        return cleanedTitle.isEmpty() ? "DefaultPage" : cleanedTitle + "Page";
    }

    private void saveToFile() {
        if (codeArea.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No code generated yet!");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Your Project's Page Objects Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath() + "/" + generatedClassName + ".java";
            try {
                Files.write(Paths.get(path), codeArea.getText().getBytes());
                JOptionPane.showMessageDialog(
                        this,
                        "Successfully exported to:\n" + path,
                        "Export Success",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Save Error: " + ex.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
