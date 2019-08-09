package com.cypress.le.mesh.meshapp;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class PopUp extends AppCompatActivity {
    ListView listView;
    TextView textView;
    String[] listItem;
    ArrayList<String>devices = new ArrayList<String>();
    ArrayList<Integer>values = new ArrayList<Integer>();
    List<HashMap<String,String>> aList = new ArrayList<HashMap<String,String>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popresult);
        showResult();
        listView=(ListView)findViewById(R.id.listView);
        textView=(TextView)findViewById(R.id.textView);

        // Keys used in Hashmap
        String[] from = { "id","value" };
        int[] to = { R.id.textView,R.id.imageView8};
        SimpleAdapter adapter = new SimpleAdapter(getBaseContext(), aList, R.layout.mylist, from, to);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    public void showResult(){
        try{
            File folder = new File(Environment.getExternalStorageDirectory()+"/Result/");
            File file = new File(folder + "/outputFile.xlsx");
            if (!folder.exists()) {
                folder.mkdir();
            }
            FileInputStream fIP = null;
            try {
                fIP = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            //Get the workbook instance for XLSX file
            XSSFWorkbook workbook = null;
            try {
                workbook = new XSSFWorkbook(fIP);
            } catch (IOException e) {
                e.printStackTrace();
            }
            XSSFSheet sheet=workbook.getSheet("Sheet Name");
            Iterator<Row> rowIter = sheet.rowIterator();
            //Loop for getting data from individual rows and columns for representation
            int rowno =0;
            while (rowIter.hasNext()) {
                Log.e("POPUP", " row no "+ rowno );
                XSSFRow myRow = (XSSFRow) rowIter.next();
                if(rowno !=0) {
                    Iterator<Cell> cellIter = myRow.cellIterator();
                    int colno =0,intvalue1=0;
                    double intvalue=0;
                    String name="", date="", value="";
                    while (cellIter.hasNext()) {
                        XSSFCell myCell = (XSSFCell) cellIter.next();
                        if (colno==0){
                            date = myCell.toString();
                        }else if (colno==1){
                            name = myCell.toString();
                            devices.add(name);

                        }else if (colno==2){
                            value = myCell.toString();
                            intvalue=Double.parseDouble(value);
                            intvalue1=(int)intvalue;

                            if (intvalue1>=75){
                            values.add(R.color.blue1); }
                            else if (intvalue1<75&&intvalue1>=50){
                                values.add(R.color.blue2);
                            }
                            else if(intvalue1<50&&intvalue1>=25){
                                values.add(R.color.blue3);
                            }
                            else if (intvalue1<25&&intvalue1>=0){
                                values.add(R.color.black);

                            }

                        }
                        colno++;
                        Log.e("POPUP", " Index :" + myCell.getColumnIndex() + " -- " + myCell.toString());
                    }
                }
                if(rowno!=0){
                    HashMap<String, String> hm = new HashMap<String,String>();
                    hm.put("id", "ID : " + devices.get(rowno-1));
                    hm.put("value",Integer.toString(values.get(rowno-1)));
                    aList.add(hm);
                }
                rowno++;


            }
        } catch (Exception e) {
            Log.e("POPUP", "error "+ e.toString());
        }
    }
}
