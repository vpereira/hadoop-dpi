package drivers;

import io.inputformat.PcapInputFormat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;

import jxta.jnetpcap.socket.JxtaSocketPerfMapper;
import jxta.jnetpcap.socket.JxtaSocketPerfReducer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SortedMapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.lib.MultipleTextOutputFormat;

@SuppressWarnings("deprecation")
public class JxtaSocketPerfDriver {

	private static int executions = 1;
	private static String inputDir = "/home/thiago/tmp/_/";	

	public static void main(String[] args) throws IOException,	InterruptedException, ClassNotFoundException {
		
		if(args != null && args.length == 2){
			executions = Integer.valueOf(args[0]);
			inputDir = args[1];
		}
		
		Configuration conf = new Configuration();
		ArrayList<Long> times = new ArrayList<Long>();
		
		// Copy local files into HDFS
		String[] files = new File(inputDir).list();
		FileSystem hdfs = FileSystem.get(conf);
		String dstDir = hdfs.getWorkingDirectory() + "/input/";
		for (int i = 0; i < files.length; i++) {
			Path srcFilePath = new Path(inputDir + files[i]);		
			hdfs.copyFromLocalFile(srcFilePath, new Path(dstDir + files[i]));
		}
		inputDir = dstDir;
		
		for (int k = 0; k < executions; k++) {			
			JobConf job = new JobConf(JxtaSocketPerfDriver.class);
			job.setJarByClass(JxtaSocketPerfMapper.class);

			// Input
			FileInputFormat.setInputPaths(job,new Path(inputDir));
			job.setInputFormat(PcapInputFormat.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(SortedMapWritable.class);

			//MapReduce Classes
			job.setMapperClass(JxtaSocketPerfMapper.class);
			job.setReducerClass(JxtaSocketPerfReducer.class);
			//		job.setNumReduceTasks(3);

			// Output
			job.setOutputFormat(MultipleFilesOutputFormat.class);			
			
			Path outputPath = new Path("output/1.9GB." + System.currentTimeMillis());
			FileOutputFormat.setOutputPath(job, outputPath);
			long t0,t1;
			t0 = System.currentTimeMillis();
			JobClient.runJob(job);
			t1 = System.currentTimeMillis();
			times.add(t1-t0);
			System.out.println("### " + k + "-Time: " + (t1-t0));	
		}

		System.out.println("### Times:");
		for (Long time : times) {
			System.out.println(time);
		}

		//process generated files
		//		updateFiles(outputPath);
	}

	private static class MultipleFilesOutputFormat extends MultipleTextOutputFormat<Text,Text>	{
		protected String generateFileNameForKeyValue(Text key,Text value,String filename){
			return key.toString();
		}
	}
	
	@SuppressWarnings("unused")
	private static void updateFiles(Path outputPath) throws FileNotFoundException, IOException {
		int i = 0;
		long min = Long.MAX_VALUE;
		String line = null;

		File rttFile = new File(outputPath.toUri() + "/" + JxtaSocketPerfMapper.jxtaRelyRttKey);				
		BufferedReader rttBuffer = new BufferedReader(new FileReader(rttFile));        
		while((line = rttBuffer.readLine()) != null) {
			i++;
			if(i == 2){
				long tmp = Long.valueOf(new StringTokenizer(line, " ").nextToken());
				if(tmp < min){
					min = tmp;
				}
			}
		}
		rttBuffer.close();
		System.out.println("### Min: " + min);
		setContents(rttFile,min);

		File arrivalFile = new File(outputPath.toUri() + "/" + JxtaSocketPerfMapper.jxtaArrivalKey);
		i = 0;
		BufferedReader arrivalBuffer = new BufferedReader(new FileReader(arrivalFile));        
		while((line = arrivalBuffer.readLine()) != null) {
			i++;
			if(i == 2){
				long tmp = Long.valueOf(new StringTokenizer(line, " ").nextToken());
				if(tmp < min){
					min = tmp;
				}
			}
		}
		arrivalBuffer.close();
		System.out.println("### Min: " + min);
		setContents(arrivalFile,min);

		File reqFile = new File(outputPath.toUri() + "/" + JxtaSocketPerfMapper.jxtaSocketReqKey);
		i = 0;
		BufferedReader reqBuffer = new BufferedReader(new FileReader(reqFile));        
		while((line = reqBuffer.readLine()) != null) {
			i++;
			if(i == 2){
				long tmp = Long.valueOf(new StringTokenizer(line, " ").nextToken());
				if(tmp < min){
					min = tmp;
				}
			}
		}
		reqBuffer.close();
		System.out.println("### Min: " + min);
		setContents(reqFile,min);

		File remFile = new File(outputPath.toUri() + "/" + JxtaSocketPerfMapper.jxtaSocketRemKey);
		i = 0;
		BufferedReader remBuffer = new BufferedReader(new FileReader(remFile));        
		while((line = remBuffer.readLine()) != null) {
			i++;
			if(i == 2){
				long tmp = Long.valueOf(new StringTokenizer(line, " ").nextToken());
				if(tmp < min){
					min = tmp;
				}
			}
		}
		remBuffer.close();
		System.out.println("### Min: " + min);
		setContents(remFile,min);
	}

	private static void setContents(File file, Long value) throws FileNotFoundException, IOException {
		if (file == null) {
			throw new IllegalArgumentException("File should not be null.");
		}
		if (!file.exists()) {
			throw new FileNotFoundException ("File does not exist: " + file);
		}
		if (!file.isFile()) {
			throw new IllegalArgumentException("Should not be a directory: " + file);
		}
		if (!file.canWrite()) {
			throw new IllegalArgumentException("File cannot be written: " + file);
		}

		String line;
		File tempFile = new File(file.getAbsolutePath() + ".txt");
		PrintWriter pw = new PrintWriter(new FileWriter(tempFile));

		BufferedReader buffer = new BufferedReader(new FileReader(file));
		buffer.readLine();
		while((line = buffer.readLine()) != null) {
			StringTokenizer tokenizer = new StringTokenizer(line, " ");
			long tmp = Long.valueOf(tokenizer.nextToken());
			tmp = tmp - value;
			StringBuilder str = new StringBuilder();
			str.append(tmp);
			str.append(" ");
			str.append(tokenizer.nextToken());
			pw.println(str);
			pw.flush();
		}
		buffer.close();
		pw.close();
	}

}