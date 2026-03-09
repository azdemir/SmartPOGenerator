package test;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * POM Scaffolder
 * Purpose: Automatically generate Selenium Page Object classes from a URL.
 */
public class PomGenerator extends JFrame {

    private static final long serialVersionUID = 1L;
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

        // --- Top Panel: Input Area ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder(" Target Configuration "));
        
        urlField = new JTextField("https://google.com");
        JButton generateBtn = new JButton("Generate Code");
        generateBtn.setPreferredSize(new Dimension(150, 30));

        topPanel.add(new JLabel(" Target URL: "), BorderLayout.WEST);
        topPanel.add(urlField, BorderLayout.CENTER);
        topPanel.add(generateBtn, BorderLayout.EAST);

        // --- Center Panel: Code Preview ---
        codeArea = new JTextArea();
        codeArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        codeArea.setEditable(false);
        codeArea.setForeground(Color.BLACK);
        JScrollPane scrollPane = new JScrollPane(codeArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(" Generated POM "));

        // --- Bottom Panel: Action Area ---
        JButton saveBtn = new JButton("Export to .java File");
        saveBtn.setFont(new Font("Arial", Font.BOLD, 14));
        saveBtn.setBackground(new Color(39, 174, 96));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setPreferredSize(new Dimension(0, 50));

        // --- Action Listeners ---
        generateBtn.addActionListener(e -> processUrl());
        saveBtn.addActionListener(e -> saveToFile());

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(saveBtn, BorderLayout.SOUTH);
   
    }

    private void processUrl() {
        try {
            String url = urlField.getText();
            Document doc = Jsoup.connect(url).get();
            generatedClassName = doc.title().replaceAll("[^a-zA-Z]", "") + "Page";
            if(generatedClassName.isEmpty()) generatedClassName = "DefaultPage";
            
            StringBuilder sb = new StringBuilder();
            sb.append("import org.openqa.selenium.WebDriver;\nimport org.openqa.selenium.WebElement;\n");
            sb.append("import org.openqa.selenium.support.FindBy;\nimport org.openqa.selenium.support.PageFactory;\n\n");
            sb.append("public class ").append(generatedClassName).append(" {\n\n");
            sb.append("    public ").append(generatedClassName).append("(WebDriver driver) {\n");
            sb.append("        PageFactory.initElements(driver, this);\n    }\n\n");

            Elements elements = doc.select("input, button, select, textarea, a");
            for (Element el : elements) {
                String id = el.id();
                String name = el.attr("name");
                String type = el.attr("type");
                String text = el.text().trim();

                String strategy;
                String val;
                String varName;

                // Priority Logic: ID > Name > XPath
                if (!id.isEmpty()) {
                    strategy = "id";
                    val = id;
                    varName = id;
                } else if (!name.isEmpty()) {
                    strategy = "name";
                    val = name;
                    varName = name;
                } else {
                    strategy = "xpath";
                    val = generateXPath(el);
                    varName = !text.isEmpty() ? text : el.tagName() + el.elementSiblingIndex();
                }

                String cleanVar = cleanVarName(varName);
                sb.append("    @FindBy(").append(strategy).append(" = \"").append(val).append("\")\n");
                sb.append("    public WebElement ").append(cleanVar).append(";\n\n");
                generateMethod(sb, el, cleanVar);
            }
            sb.append("}");
            codeArea.setText(sb.toString());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
    
 // Generates a simple, robust XPath based on tag and text or attributes
    private String generateXPath(Element el) {
        String tag = el.tagName();
        if (!el.text().isEmpty() && el.text().length() < 20) {
            return "//" + tag + "[contains(text(),'" + el.text() + "')]";
        }
        if (!el.attr("placeholder").isEmpty()) {
            return "//" + tag + "[@placeholder='" + el.attr("placeholder") + "']";
        }
        if (!el.attr("type").isEmpty()) {
            return "//" + tag + "[@type='" + el.attr("type") + "'][" + (el.elementSiblingIndex() + 1) + "]";
        }
        return "//" + tag + "[" + (el.elementSiblingIndex() + 1) + "]";
    }

    private void generateMethod(StringBuilder sb, Element el, String varName) {
        String tagName = el.tagName();
        if (tagName.equals("input") || tagName.equals("textarea")) {
            sb.append("    public void enter").append(capitalize(varName)).append("(String value) {\n");
            sb.append("        ").append(varName).append(".clear();\n");
            sb.append("        ").append(varName).append(".sendKeys(value);\n");
            sb.append("    }\n\n");
        } else if (tagName.equals("button") || (tagName.equals("input") && el.attr("type").equals("submit"))) {
            sb.append("    public void click").append(capitalize(varName)).append("() {\n");
            sb.append("        ").append(varName).append(".click();\n");
            sb.append("    }\n\n");
        }
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
                JOptionPane.showMessageDialog(this, "Successfully exported to:\n" + path, "Export Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Save Error: " + ex.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String cleanVarName(String str) {
        String cleaned = str.replaceAll("[^a-zA-Z]", "");
        return cleaned.isEmpty() ? "element" : cleaned.toLowerCase();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

}