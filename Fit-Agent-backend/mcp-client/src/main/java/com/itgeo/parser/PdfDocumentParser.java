package com.itgeo.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class PdfDocumentParser implements DocumentParser {
    @Override
    public boolean supports(String fileExtension) {
        return "pdf".equalsIgnoreCase(fileExtension);
    }

    @Override
    public String parse(String filePath) throws Exception {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
