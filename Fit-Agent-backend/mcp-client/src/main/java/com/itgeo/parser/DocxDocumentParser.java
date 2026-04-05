package com.itgeo.parser;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;

@Component
public class DocxDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileExtension) {
        return "docx".equalsIgnoreCase(fileExtension);
    }

    @Override
    public String parse(String filePath) throws Exception {
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}
