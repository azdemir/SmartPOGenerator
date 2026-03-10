# 🚀 Smart PO Generator v1.0

**Smart PO Generator** is a productivity tool designed for Test Automation Engineers.  
It automatically scans live web pages and generates **Page Object Model (POM)** classes in Java, complete with Selenium `@FindBy` annotations and ready-to-use action methods.

## ✨ Key Features

- **Smart Multi-Strategy Locators**  
  Prioritized locator logic: **ID → Name → XPath**

- **Contextual XPath Generation**  
  Builds robust, relative XPaths using `contains(text())`, `placeholder`, or other attributes

- **Method Generation**  
  Creates intuitive methods such as:  
  `fillUsername(String text)`  
  `clickLoginButton()`

- **Desktop GUI (Java Swing)**  
  Clean, user-friendly interface

- **Instant Export**  
  Save generated classes directly into your project’s `src/test/java/.../pages` directory

- **Interactive Element Detection**  
  Detects `<input>`, `<button>`, `<select>`, `<textarea>`, and `<a>` elements

# 🚀 Usage Workflow

- Launch
Run the main method in PomGeneratorApp.java
- Analyze
Enter the target URL and click Generate Code
- Review
Inspect the generated @FindBy locators and methods
- Export
Click Export to .java File, choose your project folder, and the tool will create the class using the page title as the filename.

## 🛠️ Prerequisites & Setup

### 1. Requirements

- Java JDK 11+
- Maven
- Selenium Java 4.x

### 2. Maven Dependencies

Add the following to your `pom.xml`:

```xml
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