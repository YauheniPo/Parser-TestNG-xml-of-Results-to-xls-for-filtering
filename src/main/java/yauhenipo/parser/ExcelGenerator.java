package yauhenipo.parser;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

class ExcelGenerator {

    private XSSFWorkbook workbook = new XSSFWorkbook();

    @SafeVarargs
    final void writeFileSheet(String sheetName, List<String>... columnLists) {
        XSSFSheet sheet = workbook.createSheet(sheetName);
        for (int rowNum = 0; rowNum < columnLists[0].size(); ++rowNum) {
            Row row = sheet.createRow(rowNum);
            for (int columnNum = 0; columnNum < columnLists.length; ++columnNum) {
                Cell cell = row.createCell(columnNum);
                String cellData = columnLists[columnNum].get(rowNum);
                boolean isNumericData = StringUtils.isNumeric(cellData);
                if (isNumericData) {
                    cell.setCellValue(Integer.parseInt(cellData));
                } else {
                    cell.setCellValue(columnLists[columnNum].get(rowNum));
                }
            }
        }
    }

    void createFile(File file) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            workbook.write(out);
        }
    }
}
