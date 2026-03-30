package test;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * POM Scaffolder
 * Purpose: Automatically generate Selenium Page Object classes from a URL.
 */
public class PomGenerator extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String ELEMENT_SELECTOR = "input, button, select, textarea, a";
    private static final int MAX_TEXT_LENGTH = 50;
    private static final int MAX_XPATH_ANCESTORS = 2;
    private static final int MAX_STEP_PREDICATES = 2;
    private static final List<String> CLICKABLE_INPUT_TYPES = List.of("button", "submit", "reset", "checkbox", "radio");
    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new",
            "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while"
    );
    private static final List<String> PRIORITY_ATTRIBUTES = List.of(
            "data-test-id", "data-testid", "id", "name", "aria-label", "role", "type", "placeholder", "title"
    );

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

        topPanel.add(new JLabel(" Target URL: "), BorderLayout.WEST);
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
            List<GeneratedElement> generatedElements = analyzeElements(doc);
            codeArea.setText(buildGeneratedCode(generatedElements));
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

    private List<GeneratedElement> analyzeElements(Document doc) {
        Elements elements = doc.select(ELEMENT_SELECTOR);
        List<GeneratedElement> generatedElements = new ArrayList<>();
        Set<String> usedLocatorKeys = new LinkedHashSet<>();
        Map<String, Integer> usedFieldNames = new LinkedHashMap<>();

        for (Element element : elements) {
            if (shouldSkipElement(element)) {
                continue;
            }

            GeneratedElement generated = buildGeneratedElement(doc, element, usedLocatorKeys, usedFieldNames);
            if (generated != null) {
                generatedElements.add(generated);
            }
        }

        return generatedElements;
    }

    private boolean shouldSkipElement(Element element) {
        return "hidden".equalsIgnoreCase(element.attr("type"));
    }

    private GeneratedElement buildGeneratedElement(
            Document doc,
            Element element,
            Set<String> usedLocatorKeys,
            Map<String, Integer> usedFieldNames
    ) {
        List<LocatorCandidate> candidates = buildLocatorCandidates(element);
        LocatorCandidate chosen = chooseUniqueCandidate(doc, element, candidates);
        if (chosen == null) {
            return null;
        }

        String locatorKey = chosen.strategy + "::" + chosen.value;
        if (!usedLocatorKeys.add(locatorKey)) {
            return null;
        }

        String uniqueFieldName = uniquifyFieldName(buildFieldName(chosen.preferredName, element.tagName()), usedFieldNames);
        return new GeneratedElement(element, chosen, uniqueFieldName);
    }

    private LocatorCandidate chooseUniqueCandidate(Document doc, Element element, List<LocatorCandidate> candidates) {
        for (LocatorCandidate candidate : candidates) {
            if (isUniqueMatch(doc, element, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private List<LocatorCandidate> buildLocatorCandidates(Element element) {
        Map<String, LocatorCandidate> candidates = new LinkedHashMap<>();
        String tag = element.tagName();

        addTestAttributeCandidate(candidates, element, "data-test-id", 110);
        addTestAttributeCandidate(candidates, element, "data-testid", 108);
        addIdCandidate(candidates, element, 105);
        addNameCandidate(candidates, element, 102);
        addCssAttributeCandidate(candidates, element, tag, "aria-label", 98);
        addCssAttributeCandidate(candidates, element, tag, "title", 94);
        addCssAttributeCandidate(candidates, element, tag, "placeholder", 92);
        addRoleTypeCandidates(candidates, element, tag);
        addStableClassCandidate(candidates, element, tag);
        addTextXpathCandidates(candidates, element, tag);
        addCombinedXpathCandidate(candidates, element, tag);
        addAxisBasedXpathCandidates(candidates, element, tag);
        addAncestorXpathCandidates(candidates, element, tag);

        List<LocatorCandidate> ordered = new ArrayList<>(candidates.values());
        ordered.sort((left, right) -> Integer.compare(right.score, left.score));
        return ordered;
    }

    private void addTestAttributeCandidate(
            Map<String, LocatorCandidate> candidates,
            Element element,
            String attributeName,
            int score
    ) {
        String value = cleanAttributeValue(element.attr(attributeName));
        if (value.isEmpty()) {
            return;
        }

        String selector = buildCssAttributeSelector(element.tagName(), attributeName, value);
        if (selector != null) {
            addCandidate(candidates, new LocatorCandidate("css", selector, value, score));
        }
    }

    private void addIdCandidate(Map<String, LocatorCandidate> candidates, Element element, int score) {
        String id = cleanAttributeValue(element.id());
        if (!id.isEmpty()) {
            addCandidate(candidates, new LocatorCandidate("id", id, id, score));
        }
    }

    private void addNameCandidate(Map<String, LocatorCandidate> candidates, Element element, int score) {
        String name = cleanAttributeValue(element.attr("name"));
        if (!name.isEmpty()) {
            addCandidate(candidates, new LocatorCandidate("name", name, name, score));
        }
    }

    private void addCssAttributeCandidate(
            Map<String, LocatorCandidate> candidates,
            Element element,
            String tag,
            String attributeName,
            int score
    ) {
        String value = cleanAttributeValue(element.attr(attributeName));
        if (value.isEmpty()) {
            return;
        }

        String selector = buildCssAttributeSelector(tag, attributeName, value);
        if (selector != null) {
            addCandidate(candidates, new LocatorCandidate("css", selector, value, score));
        }
    }

    private void addRoleTypeCandidates(Map<String, LocatorCandidate> candidates, Element element, String tag) {
        String role = cleanAttributeValue(element.attr("role"));
        String type = cleanAttributeValue(element.attr("type"));

        if (!role.isEmpty()) {
            String selector = buildCssAttributeSelector(tag, "role", role);
            if (selector != null) {
                addCandidate(candidates, new LocatorCandidate("css", selector, role, 90));
            }
        }

        if (!type.isEmpty()) {
            String selector = buildCssAttributeSelector(tag, "type", type);
            if (selector != null) {
                addCandidate(candidates, new LocatorCandidate("css", selector, type, 88));
            }
        }

        if (!role.isEmpty() && !type.isEmpty()) {
            String roleValue = toCssAttrLiteral(role);
            String typeValue = toCssAttrLiteral(type);
            if (roleValue != null && typeValue != null) {
                String selector = tag + "[role=" + roleValue + "][type=" + typeValue + "]";
                addCandidate(candidates, new LocatorCandidate("css", selector, role + " " + type, 91));
            }
        }
    }

    private void addStableClassCandidate(Map<String, LocatorCandidate> candidates, Element element, String tag) {
        List<String> stableClasses = extractStableClassTokens(element);
        if (stableClasses.isEmpty()) {
            return;
        }

        String selector = tag + "." + String.join(".", stableClasses);
        addCandidate(candidates, new LocatorCandidate("css", selector, String.join(" ", stableClasses), 86));
    }

    private void addTextXpathCandidates(Map<String, LocatorCandidate> candidates, Element element, String tag) {
        String text = normalizeVisibleText(element.text());
        if (text.isEmpty()) {
            return;
        }

        String literal = toXPathLiteral(text);
        addCandidate(
                candidates,
                new LocatorCandidate("xpath", "//" + tag + "[text()=" + literal + "]", text, 82)
        );

        if (text.length() >= 4) {
            addCandidate(
                    candidates,
                    new LocatorCandidate("xpath", "//" + tag + "[contains(text(), " + literal + ")]", text, 76)
            );
        }
    }

    private void addCombinedXpathCandidate(Map<String, LocatorCandidate> candidates, Element element, String tag) {
        List<String> predicates = buildSelfPredicates(element);
        if (predicates.isEmpty()) {
            return;
        }

        int maxPredicates = Math.min(3, predicates.size());
        for (int size = maxPredicates; size >= 1; size--) {
            String joined = String.join(" and ", predicates.subList(0, size));
            addCandidate(
                    candidates,
                    new LocatorCandidate("xpath", "//" + tag + "[" + joined + "]", deriveNameSeed(element), 74 + size)
            );
        }
    }

    private void addAxisBasedXpathCandidates(Map<String, LocatorCandidate> candidates, Element element, String tag) {
        String selfAxis = buildSelfAxisExpression(element, tag, 2);
        if (selfAxis.isEmpty()) {
            return;
        }

        Element parent = element.parent();
        if (parent != null) {
            List<String> parentPredicates = buildStablePredicates(parent);
            if (!parentPredicates.isEmpty()) {
                String parentStep = buildXPathStep(parent.tagName(), parentPredicates);
                addCandidate(
                        candidates,
                        new LocatorCandidate(
                                "xpath",
                                "//" + parentStep + "/child::" + selfAxis,
                                deriveNameSeed(element),
                                80
                        )
                );

                addCandidate(
                        candidates,
                        new LocatorCandidate(
                                "xpath",
                                "//" + parentStep + "/descendant::" + selfAxis,
                                deriveNameSeed(element),
                                79
                        )
                );
            }
        }

        Element previous = element.previousElementSibling();
        if (previous != null) {
            String siblingAnchor = buildSiblingAxisAnchor(previous);
            if (!siblingAnchor.isEmpty()) {
                addCandidate(
                        candidates,
                        new LocatorCandidate(
                                "xpath",
                                "//" + tag + "[preceding-sibling::" + siblingAnchor + "]",
                                deriveNameSeed(element),
                                78
                        )
                );
            }
        }

        Element next = element.nextElementSibling();
        if (next != null) {
            String siblingAnchor = buildSiblingAxisAnchor(next);
            if (!siblingAnchor.isEmpty()) {
                addCandidate(
                        candidates,
                        new LocatorCandidate(
                                "xpath",
                                "//" + tag + "[following-sibling::" + siblingAnchor + "]",
                                deriveNameSeed(element),
                                77
                        )
                );
            }
        }

        List<Element> ancestors = findStableAncestors(element);
        for (Element ancestor : ancestors) {
            List<String> ancestorPredicates = buildStablePredicates(ancestor);
            if (ancestorPredicates.isEmpty()) {
                continue;
            }

            String ancestorAxis = buildXPathStep(ancestor.tagName(), ancestorPredicates);
            addCandidate(
                    candidates,
                    new LocatorCandidate(
                            "xpath",
                            "//" + tag + "[ancestor::" + ancestorAxis + "]" + buildSelfPredicateSuffix(element, 1),
                            deriveNameSeed(element),
                            75
                    )
            );
        }
    }

    private String buildXPathStep(String tag, List<String> predicates) {
        return tag + "[" + String.join(" and ", predicates.subList(0, Math.min(MAX_STEP_PREDICATES, predicates.size()))) + "]";
    }

    private void addAncestorXpathCandidates(Map<String, LocatorCandidate> candidates, Element element, String tag) {
        List<Element> ancestors = findStableAncestors(element);
        if (ancestors.isEmpty()) {
            return;
        }

        String selfExpression = buildDescendantExpression(element, tag);
        int score = 72;
        for (Element ancestor : ancestors) {
            String ancestorExpression = buildAncestorExpression(ancestor);
            if (ancestorExpression.isEmpty()) {
                continue;
            }

            addCandidate(
                    candidates,
                    new LocatorCandidate("xpath", ancestorExpression + selfExpression, deriveNameSeed(element), score)
            );
            score -= 2;
        }
    }

    private String buildCssAttributeSelector(String tag, String attributeName, String value) {
        String literal = toCssAttrLiteral(value);
        if (literal == null) {
            return null;
        }
        return tag + "[" + attributeName + "=" + literal + "]";
    }

    private List<String> buildSelfPredicates(Element element) {
        List<String> predicates = new ArrayList<>();

        for (String attribute : PRIORITY_ATTRIBUTES) {
            String value = cleanAttributeValue(attribute.equals("id") ? element.id() : element.attr(attribute));
            if (!value.isEmpty()) {
                predicates.add("@" + attribute + "=" + toXPathLiteral(value));
            }
        }

        List<String> stableClasses = extractStableClassTokens(element);
        if (!stableClasses.isEmpty()) {
            String classPredicate = String.join(" and ", stableClasses.stream()
                    .map(token -> "contains(concat(' ', normalize-space(@class), ' '), ' " + escapeXPathText(token) + " ')")
                    .toList());
            predicates.add(classPredicate);
        }

        String text = normalizeVisibleText(element.text());
        if (!text.isEmpty()) {
            predicates.add("contains(text(), " + toXPathLiteral(text) + ")");
        }

        return predicates;
    }

    private String buildSelfAxisExpression(Element element, String tag, int predicateCount) {
        List<String> predicates = buildSelfPredicates(element);
        StringBuilder expression = new StringBuilder(tag);
        expression.append("[self::").append(tag).append("]");
        if (!predicates.isEmpty()) {
            expression.append("[")
                    .append(String.join(" and ", predicates.subList(0, Math.min(predicateCount, predicates.size()))))
                    .append("]");
        }
        return expression.toString();
    }

    private String buildSelfPredicateSuffix(Element element, int predicateCount) {
        List<String> predicates = buildSelfPredicates(element);
        if (predicates.isEmpty()) {
            return "";
        }

        return "[" + String.join(" and ", predicates.subList(0, Math.min(predicateCount, predicates.size()))) + "]";
    }

    private String buildSiblingAxisAnchor(Element sibling) {
        List<String> predicates = buildStablePredicates(sibling);
        if (predicates.isEmpty()) {
            String text = normalizeVisibleText(sibling.text());
            if (text.isEmpty()) {
                return "";
            }
            return sibling.tagName() + "[text()=" + toXPathLiteral(text) + "]";
        }

        return buildXPathStep(sibling.tagName(), predicates);
    }

    private List<Element> findStableAncestors(Element element) {
        List<Element> ancestors = new ArrayList<>();
        Element current = element.parent();

        while (current != null && ancestors.size() < MAX_XPATH_ANCESTORS) {
            if (!buildStablePredicates(current).isEmpty()) {
                ancestors.add(current);
            }
            current = current.parent();
        }

        return ancestors;
    }

    private String buildAncestorExpression(Element ancestor) {
        List<String> predicates = buildStablePredicates(ancestor);
        if (predicates.isEmpty()) {
            return "";
        }

        return "//" + buildXPathStep(ancestor.tagName(), predicates);
    }

    private List<String> buildStablePredicates(Element element) {
        List<String> predicates = new ArrayList<>();

        for (String attribute : PRIORITY_ATTRIBUTES) {
            String value = cleanAttributeValue(attribute.equals("id") ? element.id() : element.attr(attribute));
            if (!value.isEmpty()) {
                predicates.add("@" + attribute + "=" + toXPathLiteral(value));
            }
        }

        List<String> stableClasses = extractStableClassTokens(element);
        if (!stableClasses.isEmpty()) {
            predicates.add("contains(concat(' ', normalize-space(@class), ' '), ' " + escapeXPathText(stableClasses.get(0)) + " ')");
        }

        return predicates;
    }

    private String buildDescendantExpression(Element element, String tag) {
        List<String> predicates = buildSelfPredicates(element);
        if (predicates.isEmpty()) {
            return "//" + tag;
        }

        return "//" + buildXPathStep(tag, predicates);
    }

    private boolean isUniqueMatch(Document doc, Element element, LocatorCandidate candidate) {
        try {
            Elements matches = switch (candidate.strategy) {
                case "id" -> doc.select("[id=" + toCssAttrLiteral(candidate.value) + "]");
                case "name" -> doc.select("[name=" + toCssAttrLiteral(candidate.value) + "]");
                case "css" -> doc.select(candidate.value);
                case "xpath" -> doc.selectXpath(candidate.value);
                default -> new Elements();
            };

            return matches.size() == 1 && matches.get(0) == element;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String buildGeneratedCode(List<GeneratedElement> generatedElements) {
        StringBuilder sb = new StringBuilder();
        Set<String> emittedFields = new LinkedHashSet<>();
        Set<String> emittedMethods = new LinkedHashSet<>();
        sb.append("import org.openqa.selenium.WebDriver;\n");
        sb.append("import org.openqa.selenium.WebElement;\n");
        sb.append("import org.openqa.selenium.support.FindBy;\n\n");
        sb.append("public class ").append(generatedClassName).append(" {\n\n");
        sb.append("    private final WebDriver driver;\n\n");
        sb.append("    public ").append(generatedClassName).append("(WebDriver driver) {\n");
        sb.append("        this.driver = driver;\n");
        sb.append("    }\n\n");

        for (GeneratedElement generated : generatedElements) {
            appendField(sb, generated, emittedFields);
        }

        for (GeneratedElement generated : generatedElements) {
            generateMethod(sb, generated.element, generated.fieldName, emittedMethods);
        }

        sb.append("}");
        return sb.toString();
    }

    private void appendField(StringBuilder sb, GeneratedElement generated, Set<String> emittedFields) {
        String fieldBlock = "    @FindBy(" +
                generated.primaryLocator.strategy +
                " = \"" +
                escapeJava(generated.primaryLocator.value) +
                "\")\n" +
                "    public WebElement " +
                generated.fieldName +
                ";\n\n";
        if (emittedFields.add(fieldBlock)) {
            sb.append(fieldBlock);
        }
    }

    private void generateMethod(StringBuilder sb, Element element, String fieldName, Set<String> emittedMethods) {
        String tagName = element.tagName();
        String inputType = element.attr("type").toLowerCase(Locale.ROOT);

        if ("input".equals(tagName) && isClickableInput(inputType)) {
            appendClickMethod(sb, fieldName, emittedMethods);
            return;
        }

        if ("input".equals(tagName) || "textarea".equals(tagName)) {
            appendEnterMethod(sb, fieldName, emittedMethods);
            return;
        }

        if ("button".equals(tagName) || "a".equals(tagName)) {
            appendClickMethod(sb, fieldName, emittedMethods);
        }
    }

    private boolean isClickableInput(String inputType) {
        return CLICKABLE_INPUT_TYPES.contains(inputType);
    }

    private void appendEnterMethod(StringBuilder sb, String fieldName, Set<String> emittedMethods) {
        String methodBlock = "    public void enter" + capitalize(fieldName) + "(String value) {\n" +
                "        " + fieldName + ".clear();\n" +
                "        " + fieldName + ".sendKeys(value);\n" +
                "    }\n\n";
        if (emittedMethods.add(methodBlock)) {
            sb.append(methodBlock);
        }
    }

    private void appendClickMethod(StringBuilder sb, String fieldName, Set<String> emittedMethods) {
        String methodBlock = "    public void click" + capitalize(fieldName) + "() {\n" +
                "        " + fieldName + ".click();\n" +
                "    }\n\n";
        if (emittedMethods.add(methodBlock)) {
            sb.append(methodBlock);
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

    private void addCandidate(Map<String, LocatorCandidate> candidates, LocatorCandidate candidate) {
        candidates.putIfAbsent(candidate.strategy + "::" + candidate.value, candidate);
    }

    private String buildFieldName(String raw, String fallbackPrefix) {
        String candidate = raw == null ? "" : raw.trim();
        if (candidate.isEmpty()) {
            candidate = fallbackPrefix;
        }

        String[] tokens = candidate
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("[^a-zA-Z0-9]+", " ")
                .trim()
                .split("\\s+");

        StringBuilder fieldName = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }

            String normalized = token.toLowerCase(Locale.ROOT);
            if (fieldName.isEmpty()) {
                fieldName.append(normalized);
            } else {
                fieldName.append(capitalize(normalized));
            }
        }

        if (fieldName.isEmpty()) {
            fieldName.append(fallbackPrefix.toLowerCase(Locale.ROOT));
        }

        if (Character.isDigit(fieldName.charAt(0))) {
            fieldName.insert(0, fallbackPrefix.toLowerCase(Locale.ROOT));
        }

        if (JAVA_KEYWORDS.contains(fieldName.toString())) {
            fieldName.append("Field");
        }

        return fieldName.toString();
    }

    private String uniquifyFieldName(String baseName, Map<String, Integer> usedFieldNames) {
        int nextCount = usedFieldNames.getOrDefault(baseName, 0) + 1;
        usedFieldNames.put(baseName, nextCount);
        return nextCount == 1 ? baseName : baseName + nextCount;
    }

    private String deriveNameSeed(Element element) {
        for (String attribute : PRIORITY_ATTRIBUTES) {
            String value = cleanAttributeValue(attribute.equals("id") ? element.id() : element.attr(attribute));
            if (!value.isEmpty()) {
                return value;
            }
        }

        String text = normalizeVisibleText(element.text());
        if (!text.isEmpty()) {
            return text;
        }

        List<String> stableClasses = extractStableClassTokens(element);
        if (!stableClasses.isEmpty()) {
            return stableClasses.get(0);
        }

        return element.tagName();
    }

    private List<String> extractStableClassTokens(Element element) {
        List<String> stable = new ArrayList<>();
        for (String token : element.classNames()) {
            if (isStableClassToken(token)) {
                stable.add(token);
            }
        }
        return stable;
    }

    private boolean isStableClassToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        if (token.length() < 3 || token.length() > 40) {
            return false;
        }
        if (Pattern.compile(".*\\d{4,}.*").matcher(token).matches()) {
            return false;
        }
        return !token.contains("__") && !token.startsWith("css-") && !token.startsWith("jss");
    }

    private String normalizeVisibleText(String text) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty() || normalized.length() > MAX_TEXT_LENGTH) {
            return "";
        }
        return normalized;
    }

    private String cleanAttributeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String toCssAttrLiteral(String value) {
        if (value.contains("'")) {
            if (value.contains("\"")) {
                return null;
            }
            return "\"" + value + "\"";
        }
        return "'" + value + "'";
    }

    private String toXPathLiteral(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }

        String[] parts = value.split("'");
        StringBuilder literal = new StringBuilder("concat(");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                literal.append(", \"'\", ");
            }
            literal.append("'").append(parts[i]).append("'");
        }
        literal.append(")");
        return literal.toString();
    }

    private String escapeXPathText(String value) {
        return value.replace("'", "\\'");
    }

    private String escapeJava(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1);
    }

    private static final class LocatorCandidate {
        private final String strategy;
        private final String value;
        private final String preferredName;
        private final int score;

        private LocatorCandidate(String strategy, String value, String preferredName, int score) {
            this.strategy = strategy;
            this.value = value;
            this.preferredName = preferredName;
            this.score = score;
        }
    }

    private static final class GeneratedElement {
        private final Element element;
        private final LocatorCandidate primaryLocator;
        private final String fieldName;

        private GeneratedElement(
                Element element,
                LocatorCandidate primaryLocator,
                String fieldName
        ) {
            this.element = element;
            this.primaryLocator = primaryLocator;
            this.fieldName = fieldName;
        }
    }
}
