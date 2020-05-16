package com.noamza.nrdreader;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Hashtable;
import java.util.*;
import java.text.*;

//Based on specs found at http://neuralynx.com/software/NeuralynxDataFileFormats.pdf


public class Main {
    static String homeDir = "C:\\Noam\\Data\\doriPlot\\"; //  Ohed\\160519\\"; // "160515"
    static String[] inputDirs = { //list of folders
                                 "test"//"1" + "\\", "2" + "\\", "3" + "\\", "4" + "\\"
                                };


    static String inputFile = "DigitalLynxSXRawDataFile_01.nrd";
    //150129_1DigitalLynxSXRawDataFile_01.nrd

    static boolean segment = true; //ON OFF
    static int numChannels = 16;

    public static void main(String[] args) {
        /*
        for(int i = 0; i < numChannels; i++){
            System.out.printf("ch%d min %d max %d\n", i, i, i);
        }
        while(numChannels==16){}
        */
        /**/
        for(String inputDir: inputDirs) {

            String outputDir = homeDir + inputDir;
            System.out.println("Hello " + inputDir);
            InputStream inStream = null;
            BufferedInputStream bis = null;
            String fin = homeDir + inputDir + inputFile;
            FileWriter fwo = null;
            //BufferedWriter out = null;
            String cols[] = {"STX", "ID", "Size", "TimestampHigh", "TimestampLow", "Status",
                    "ParallelPort", "Extras", "Data", "CRC"};
            int fakeTime = 1;
            BufferedWriter[] outa = new BufferedWriter[numChannels];
            int maxLinesPerFile = 1000000;
            int chunk = 1;

            if(segment) outputDir += "chunks" + "\\";

            int[] minCh = new int[numChannels], maxCh = new int[numChannels];
            for(int i = 0; i < numChannels; i++){
                minCh[i] = Integer.MAX_VALUE;
                maxCh[i] = Integer.MIN_VALUE;
            }

            try {
                inStream = new FileInputStream(fin);
                bis = new BufferedInputStream(inStream);
                int stop = 0;
                int lines = 0;


                // read until a single byte is available
                while (bis.available() > 0 && stop < 10000 ) {
                    //stop++; //STOP
                    byte[] arr = new byte[4];
                    bis.read(arr, 0, 4);
                    ByteBuffer bb = ByteBuffer.wrap(arr);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    int temp = bb.getInt();
                    // print the characters
                    //System.out.println("Int32: "+ temp);
                    //Start of packet
                    //*


                    if (temp == 2048) {
                        Hashtable<String, Object> packet = new Hashtable<String, Object>();
                        packet.put("STX", new Integer(temp));
                        arr = new byte[4];
                        bis.read(arr, 0, 4);
                        bb = ByteBuffer.wrap(arr);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        packet.put("ID", new Integer(bb.getInt()));
                        arr = new byte[4];
                        bis.read(arr, 0, 4);
                        bb = ByteBuffer.wrap(arr);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        Integer dataSize = new Integer(bb.getInt());
                        packet.put("Size", dataSize);
                        arr = new byte[4];
                        bis.read(arr, 0, 4);
                        bb = ByteBuffer.wrap(arr);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        packet.put("TimestampHigh", new Long(bb.getInt()));
                        arr = new byte[4];
                        bis.read(arr, 0, 4);
                        bb = ByteBuffer.wrap(arr);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        packet.put("TimestampLow", new Long(bb.getInt()));
                        arr = new byte[4];
                        bis.read(arr, 0, 4);
                        bb = ByteBuffer.wrap(arr);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        packet.put("Status", new Integer(bb.getInt()));
                        arr = new byte[4];
                        bis.read(arr, 0, 4);
                        bb = ByteBuffer.wrap(arr);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        packet.put("ParallelPort", new Integer(bb.getInt()));

                        Integer extras[] = new Integer[10];
                        for (int i = 0; i < 10; i++) {
                            arr = new byte[4];
                            bis.read(arr, 0, 4);
                            bb = ByteBuffer.wrap(arr);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            extras[i] = new Integer(bb.getInt());
                        }
                        packet.put("Extras", extras);


                        //ASSUMES DATA HAS 64 length!!
                        Integer data[] = new Integer[dataSize - 10]; //
                        for (int i = 0; i < dataSize - 10; i++) {
                            arr = new byte[4];
                            bis.read(arr, 0, 4);
                            bb = ByteBuffer.wrap(arr);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            data[i] = new Integer(bb.getInt());
                        }
                        packet.put("Data", data);

                        //arr = new byte[4];bis.read(arr, 0, 4); bb = ByteBuffer.wrap(arr);bb.order(ByteOrder.LITTLE_ENDIAN);
                        //packet.put("Data", new Integer(bb.getInt()));
                        arr = new byte[4];
                        bis.read(arr, 0, 4);
                        bb = ByteBuffer.wrap(arr);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        packet.put("CRC", new Integer(bb.getInt()));

                        for (String k : cols) {
                            //print(k+": "+packet.get(k));
                        }
                        for (int k : (Integer[]) packet.get("Extras")) {
                            //print("extra: "+k);
                        }
                        for (int k : (Integer[]) packet.get("Data")) {
                            //print(i++ + " data: "+k);
                        }

                        long timestamp = (long) packet.get("TimestampHigh");
                        //Integer.toUnsignedLong((int) packet.get("TimestampHigh"));
                        //print("time = " + timestamp);
                        timestamp <<= 32;
                        //print("time = " + timestamp);
                        timestamp += (long) packet.get("TimestampLow");
                        //Integer.toUnsignedLong((int)packet.get("TimestampLow"));
                        //print("time = " + timestamp);
                        //Date time = new Date(timestamp / 1000); //In Microseconds
                        //DateFormat formatter = new SimpleDateFormat("YY:MM:DD:HH:mm:ss:SSS");
                        //String dateFormatted = formatter.format(time);
                        //print(time);

                        if (lines == maxLinesPerFile && segment) {
                            //out.close();
                            for (int i = 0; i < numChannels; i++) {
                                outa[i].close();
                            }
                            chunk += 1;
                            lines = 0;
                        }

                        if (fakeTime % maxLinesPerFile == 0) {
                            print("processing: " + fakeTime + " chunk " + chunk + "! " + (new Date()).toString());
                        }

                        if (lines == 0) {
                            String name = timestamp + ".csv";
                            //fwo = new FileWriter(outputDir+name);
                            //channel files.
                            for (int i = 0; i < numChannels; i++) {
                                String filename = "";
                                if(segment){
                                    filename = outputDir + "ch" + (i + 1) + "_" + chunk + ".csv";
                                } else {
                                    filename = outputDir + "ch" + (i + 1) + ".csv";
                                }
                                outa[i] = new BufferedWriter(new FileWriter(filename));
                            }
                            //out = new BufferedWriter(fwo);
                            String header = "time";
                            for (int i = 0; i < dataSize - 10; i++) {
                                header += ",ch" + (i + 1);
                            }
                            header += "\n";
                            //print(header);
                            //write(out,header); //WRITE
                        }

                        String output = timestamp + "";
                        for (int k : (Integer[]) packet.get("Data")) {
                            output += "," + k;
                        }
                        output += "\n";
                        //print(output);

                        //write(out, output); WRITE

                        //write out each channel seperately
                        for (int i = 0; i < numChannels; i++) {
                            int chVal = ((Integer[]) packet.get("Data"))[i];
                            if(minCh[i]>chVal) minCh[i] = chVal;
                            if(maxCh[i]<chVal) maxCh[i] = chVal;
                            output = "";
                            if (segment) {
                                output = fakeTime + "," + chVal + "\n";
                            } else {
                                output = chVal + "\n";
                            }
                            write(outa[i], output);
                            //print(output);
                        }

                        lines++;
                        fakeTime++;
                        

                    } /**/

                }
            } catch (Exception e) {
                // if any I/O error occurs
                e.printStackTrace();
            } finally {
                // releases any system resources associated with the streams
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (Exception e) {
                        print("error closing");
                    }
                }
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (Exception e) {
                        print("error closing");
                    }
                }
                if (fwo != null) {
                    try {
                        fwo.close();
                    } catch (Exception e) {
                        print("error closing");
                    }
                }
            /*
            if(out!=null) {
                try {
                    out.close();
                } catch (Exception e ) {
                    print("error closing");
                }
            }
            */
                for (int i = 0; i < numChannels; i++) {
                    if (outa[i] != null) {
                        try {
                            outa[i].close();
                        } catch (Exception e) {
                            print("error closing");
                        }
                    }
                }
            }
            print(inputDir+":");
            for(int i = 0; i < numChannels; i++){
                System.out.printf("ch%d min %d max %d\n", i, minCh[i], maxCh[i]);
                //System.out.printf("ch%d min %d max %d\n", i, i, i);
            }
            print("************");

        } // END INPUT DIRS
        print("done!");
    }

    public static void print(Object o) {
        System.out.println(o.toString());
    }

    public static void write(BufferedWriter out, String s){
        try{
            out.write(s);
            //out.close();
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

    }

}
                        /*
                    long t = 1;
                    print(t);
                     t <<= 1;
                    print(t);
                     t <<= 1;
                    print(t);
                     t <<= 1;
                    print(t);
                    t <<= 5;
                    print(t); */


//break;

/*

"C:\Program Files\Java\jdk1.8.0_51\bin\java" -Didea.launcher.port=7533 "-Didea.launcher.bin.path=C:\Program Files (x86)\JetBrains\IntelliJ IDEA Community Edition 14.1.4\bin" -Dfile.encoding=windows-1255 -classpath "C:\Program Files\Java\jdk1.8.0_51\jre\lib\charsets.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\deploy.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\javaws.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\jce.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\jfr.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\jfxswt.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\jsse.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\management-agent.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\plugin.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\resources.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\rt.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\ext\access-bridge-64.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\ext\cldrdata.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\ext\dnsns.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\ext\jaccess.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\ext\jfxrt.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\ext\localedata.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\ext\nashorn.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\ext\sunec.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\ext\sunjce_provider.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\ext\sunmscapi.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\ext\sunpkcs11.jar;C:\Program Files\Java\jdk1.8.0_51\jre\lib\ext\zipfs.jar;C:\Users\alm\Desktop\NRDReader\out\production\NRDReader;C:\Program Files (x86)\JetBrains\IntelliJ IDEA Community Edition 14.1.4\lib\idea_rt.jar" com.intellij.rt.execution.application.AppMain com.noamza.nrdreader.Main
Hello 2015-01-01_15-45-34_dreadd_rat_ref_animalground_2100depth\
processing: 1000000 chunk 1! Sun Aug 16 18:35:44 IDT 2015
done!

"C:\Program Files\Java\jdk1.8.0_92\bin\java" -Didea.launcher.port=7533 "-Didea.launcher.bin.path=C:\Program Files (x86)\JetBrains\IntelliJ IDEA Community Edition 2016.1.2\bin" -Dfile.encoding=UTF-8 -classpath "C:\Program Files\Java\jdk1.8.0_92\jre\lib\charsets.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\deploy.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\ext\access-bridge-64.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\ext\cldrdata.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\ext\dnsns.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\ext\jaccess.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\ext\jfxrt.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\ext\localedata.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\ext\nashorn.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\ext\sunec.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\ext\sunjce_provider.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\ext\sunmscapi.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\ext\sunpkcs11.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\ext\zipfs.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\javaws.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\jce.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\jfr.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\jfxswt.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\jsse.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\management-agent.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\plugin.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\resources.jar;C:\Program Files\Java\jdk1.8.0_92\jre\lib\rt.jar;C:\Noam\Dropbox\GitTechnion\javaDoriDataReader\out\production\NRDReader;C:\Program Files (x86)\JetBrains\IntelliJ IDEA Community Edition 2016.1.2\lib\idea_rt.jar" com.intellij.rt.execution.application.AppMain com.noamza.nrdreader.Main
Hello 1\
1\:
ch0 min 1817694 max 2248301
ch1 min 1813464 max 2272858
ch2 min 1793322 max 2201764
ch3 min 1811031 max 2276906
ch4 min 1963772 max 2313268
ch5 min 1999340 max 2290264
ch6 min 1852381 max 2168460
ch7 min 1862387 max 2166669
ch8 min 1867412 max 2311753
ch9 min 1833966 max 2283821
ch10 min -8388608 max 5212898
ch11 min 1841096 max 2282118
ch12 min 1871522 max 2328152
ch13 min 1863795 max 2335535
ch14 min -1647817 max 4143673
ch15 min 1846720 max 2305478
************
Hello 2\
2\:
ch0 min 2045778 max 2492974
ch1 min 2033148 max 2495240
ch2 min 2007335 max 2438928
ch3 min 2043981 max 2513857
ch4 min 2107911 max 2462635
ch5 min 2155226 max 2487058
ch6 min 1994109 max 2313017
ch7 min 1998142 max 2330478
ch8 min 2061803 max 2532550
ch9 min 2053554 max 2525257
ch10 min -8388608 max 8388607
ch11 min 2044142 max 2501040
ch12 min 2066186 max 2538002
ch13 min 2069211 max 2545972
ch14 min -1254707 max 5381513
ch15 min 1978760 max 2484458
************
Hello 3\
3\:
ch0 min 1834452 max 2290285
ch1 min 1828816 max 2278153
ch2 min 1810583 max 2338925
ch3 min 1833275 max 2349718
ch4 min 1961067 max 2282365
ch5 min 2006112 max 2290823
ch6 min 1839456 max 2156854
ch7 min 1860673 max 2220375
ch8 min 1880131 max 2370608
ch9 min 1850509 max 2346854
ch10 min -6204637 max 8388607
ch11 min 1858783 max 2302027
ch12 min 1881221 max 2327126
ch13 min 1878774 max 2465538
ch14 min -774033 max 5650384
ch15 min 1857054 max 2308935
************
Hello 4\
4\:
ch0 min 1819713 max 2295572
ch1 min 1816398 max 2342690
ch2 min 1794792 max 2194295
ch3 min 1817531 max 2267095
ch4 min 1957979 max 2254602
ch5 min 1975741 max 2249046
ch6 min 1846868 max 2081196
ch7 min 1843988 max 2085949
ch8 min 1856626 max 2322492
ch9 min 1830792 max 2300156
ch10 min -4259083 max 4094355
ch11 min 1842838 max 2294658
ch12 min 1867039 max 2322005
ch13 min 1864397 max 2328661
ch14 min -10453 max 3882448
ch15 min 1819406 max 2275314
************
done!

Process finished with exit code 0



 */