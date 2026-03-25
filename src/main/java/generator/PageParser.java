package generator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PageParser {

    public List<Element> parse(String url) throws Exception {

        Document doc;

        if (url.startsWith("file:///")) {
            String filePath = url.substring(7); // strip "file://" leaving "/path/to/file"
            doc = Jsoup.parse(new File(filePath), "UTF-8");
        } else if (url.startsWith("file://")) {
            String filePath = url.substring(7);
            doc = Jsoup.parse(new File(filePath), "UTF-8");
        } else if (url.startsWith("http://") || url.startsWith("https://")) {
            doc = Jsoup.connect(url).get();
        } else {
            throw new Exception("Only http, https, and file:// protocols are supported");
        }

        Elements elements = doc.select(
                "input,button,select,textarea,a"
        );

        return new ArrayList<>(elements);
    }
}