package yauhenipo.parser;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * The type Excel generator.
 * @author Yauheni Papovich
 */
@NoArgsConstructor
public class ExcelGenerator {

    private XSSFWorkbook workbook = new XSSFWorkbook();
    public static final String REPORT_SHEET = "report";
    static final String SUMMARY_SHEET = "summary";
    static final String EXCEL_EXTENSION = ".xlsx";

    public ExcelGenerator(String path) throws IOException, InvalidFormatException {
        OPCPackage pkg = OPCPackage.open(path);
        workbook = new XSSFWorkbook(pkg);
    }

    @SafeVarargs
    final void writeDataToExcelSheet(String sheetName, List<String>... columnLists) {
        XSSFSheet sheet = workbook.createSheet(sheetName);
        for (int rowNum = 0; rowNum < columnLists[0].size(); ++rowNum) {
            Row row = sheet.createRow(rowNum);
            for (int columnNum = 0; columnNum < columnLists.length; ++columnNum) {
                Cell cell = row.createCell(columnNum);
                String cellData = columnLists[columnNum].get(rowNum);
                if (StringUtils.isNumeric(cellData)) {
                    cell.setCellValue(Integer.parseInt(cellData));
                } else {
                    cell.setCellValue(StringUtils.substring(cellData, 0, 32767));
                }
            }
        }
    }

    void createFile(File file) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            workbook.write(out);
        }
    }

    public XSSFSheet getSheet(String sheetName) {
        return workbook.getSheet(sheetName);
    }
}
