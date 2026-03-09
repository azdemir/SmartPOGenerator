package generator;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class MainApp extends JFrame {

    private static final long serialVersionUID = 1L;
	private JTextField urlField;
    private JTextArea output;

    public MainApp() {

        setTitle("Selenium POM Generator");
        setSize(900,600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());

        urlField = new JTextField("https://example.com");
        JButton generate = new JButton("Generate");

        top.add(new JLabel("URL: "), BorderLayout.WEST);
        top.add(urlField, BorderLayout.CENTER);
        top.add(generate, BorderLayout.EAST);

        output = new JTextArea();
        JScrollPane scroll = new JScrollPane(output);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        generate.addActionListener(e -> generateCode());
    }

    private void generateCode() {

        try {

            String url = urlField.getText();

            PageParser parser = new PageParser();
            var elements = parser.parse(url);

            CodeGenerator generator = new CodeGenerator();
            String code = generator.generate("GeneratedPage", elements);

            output.setText(code);

        } catch(Exception ex) {
            output.setText(ex.getMessage());
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new MainApp().setVisible(true);
        });

    }
}