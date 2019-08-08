package com.example.tmankita.check4u.Utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Compare2CSVTables {
    String pathFile1;
    String pathFile2;
    HashMap<Integer,Integer> statisticsOfErrors;
    int total;
    public Compare2CSVTables(String pathFile1, String pathFile2) {
        this.pathFile1 = pathFile1;
        this.pathFile2 = pathFile2;
        this.statisticsOfErrors = new HashMap<>();
    }
    public String compare (){
        List<String[]> allElementsCSV1=null;
        List<String[]> allElementsCSV2=null;
        try{
            CSVReader reader1 = new CSVReader(new FileReader(pathFile1));
            allElementsCSV1 = reader1.readAll();
            reader1.close();
        }catch (Exception e){
            Log.e("Compare csv1", "compare: not succeeded  to create CSVReader", e);
        }
        try{
            CSVReader reader2 = new CSVReader(new FileReader(pathFile2));
            allElementsCSV2 = reader2.readAll();
            reader2.close();
        }catch (Exception e){
            Log.e("Compare csv2", "compare: not succeeded  to create CSVReader", e);
        }

        for (String[] line1 : allElementsCSV1) {
          String id1 = line1[0];
          String id_visual1 = id1.substring(0,6);
            for (String[] line2 : allElementsCSV2) {
                String id2 = line2[0];
                String id_visual2 = id2.substring(0,6);
                if(id_visual1.equals(id_visual2)){
                    int er = compareLines(line1,line2);
                    total+=er;
                    break;
                }
            }
        }

        try{
            File exportDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Check4U_DB");
            File csvDir = new File(exportDir.getPath(),"CSV");

            File file = new File(csvDir, "Check4U_Compare2csvTest"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())+".csv");
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            csvWrite.writeNext(allElementsCSV1.get(0));

            String[] row = new String[statisticsOfErrors.size()];
            int i=0;
            for (Integer numberOfErrors: statisticsOfErrors.values()) {
                row[i] = String.valueOf((numberOfErrors.intValue()/total)*10);
            }
            csvWrite.writeNext(row);
            csvWrite.close();
            return file.getAbsolutePath();

        } catch(Exception e){
            Log.e("Compare csv2", "compare: not succeeded  to create CSVWrite", e);
        }
        return null;
    }

    private int compareLines(String[] line1, String[] line2){
        int er = 0;
        Integer qNumber = new Integer(1);
        for (int i = 1; i <line1.length ; i++) {
                if(!line1[i].equals(line2[i])){
                    er++;
                    if(statisticsOfErrors.containsKey(qNumber)){
                        int prev = statisticsOfErrors.get(qNumber).intValue();
                        prev++;
                        statisticsOfErrors.remove(qNumber);
                        statisticsOfErrors.put(qNumber,new Integer(prev));
                    }
                    else{
                        statisticsOfErrors.put(qNumber,new Integer(1));

                    }
                    qNumber++;
                }
            }
        return er;
        }
    }

