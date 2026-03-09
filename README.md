🚀 Gemini POM Scaffolder v3.5
Gemini POM Scaffolder is a specialized productivity tool for Test Automation Engineers. It automates the creation of Page Object Model (POM) classes by scanning live web pages and instantly generating Java code with Selenium @FindBy annotations and action methods.
✨ Key Features
    •    Smart Multi-Strategy Locators: Uses a prioritized selection logic: ID > Name > XPath.
    •    Contextual XPath Generation: Automatically creates robust, relative XPaths using contains(text()), placeholder, or attributes when standard IDs are missing.
    •    Boilerplate Method Generation: Produces ready-to-use methods like fillUsername(String text) or clickLoginButton() based on element type.
    •    Desktop GUI: A clean, user-friendly interface built with Java Swing for rapid interaction.
    •    Instant Export: Save generated classes directly to your project's src/test/java/.../pages directory.
    •    Interactive Element Detection: Scans for input, button, select, textarea, and <a> (links).

🛠️ Prerequisites & Setup
1. Requirements
    •    Java JDK: 11 or higher.
    •    Maven: For managing dependencies.
    •    Selenium: Ensure your test project uses selenium-java 4.x.
2. Maven Dependencies
Add the following to your pom.xml to enable HTML parsing and Selenium support:
XML

<dependencies>
    <dependency>
        <groupId>org.jsoup</groupId>
        <artifactId>jsoup</artifactId>
        <version>1.15.4</version>
    </dependency>
    
    <dependency>
        <groupId>org.openqa.seleniumhq.selenium</groupId>
        <artifactId>selenium-java</artifactId>
        <version>4.18.1</version>
    </dependency>
</dependencies>

🚀 Usage Workflow
    1    Launch: Run the main method in PomGeneratorApp.java.
    2    Analyze: Enter the target URL and click "Generate Code".
    3    Review: Examine the generated @FindBy strategies in the preview window.
    4    Export: Click "Export to .java File", select your project folder, and the tool creates the class with the correct filename based on the page title.

