package fr.turgot.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LinkHelper {

    public static final String JSP_ROOT = "/WEB-INF/";

    public static String jspLink(String relativePath) {
        return "page?path=" + relativePath;
    }

    public static List<String> listAllJSP(String webappRoot) {
        List<String> pages = new ArrayList<>();
        File rootDir = new File(webappRoot + JSP_ROOT);
        scanDirectory(rootDir, "", pages);
        return pages;
    }

    private static void scanDirectory(File dir, String pathPrefix, List<String> pages) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    scanDirectory(file, pathPrefix + file.getName() + "/", pages);
                } else if (file.getName().endsWith(".jsp")) {
                    String name = file.getName().replace(".jsp", "");
                    pages.add(pathPrefix + name);
                }
            }
        }
    }
}
