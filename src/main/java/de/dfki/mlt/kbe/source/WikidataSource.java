/**
 *
 */
package de.dfki.mlt.kbe.source;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.json.JSONObject;

/**
 * @author Aydan Rende, DFKI
 *
 */
public class WikidataSource implements SourceFunction<JSONObject> {
	private static final long serialVersionUID = 1L;

	private volatile boolean isRunning = true;
	private String inputFilePath;

	public WikidataSource(String inputFilePath) {
		this.inputFilePath = inputFilePath;
	}

	@Override
	public void run(SourceFunction.SourceContext<JSONObject> context)
			throws IOException, CompressorException {
		BufferedReader reader = getBufferedReaderForCompressedFile(inputFilePath);
		String line = new String();
		JSONObject jsonObject = new JSONObject();
		if (reader != null) {
			while ((line = reader.readLine()) != null && isRunning) {
				if (!(line.equals("[") || line.equals("]"))) {
					jsonObject = new JSONObject(line);
					context.collect(jsonObject);
				}
			}
		}
		reader.close();
	}

	@Override
	public void cancel() {
		isRunning = false;
	}

	public BufferedReader getBufferedReaderForCompressedFile(String fileIn)
			throws FileNotFoundException, CompressorException {
		FileInputStream inputStream = new FileInputStream(fileIn);
		BufferedInputStream bufferedStream = new BufferedInputStream(
				inputStream);
		CompressorInputStream compressorStream = new CompressorStreamFactory()
				.createCompressorInputStream(bufferedStream);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				compressorStream, StandardCharsets.UTF_8));
		return reader;
	}

}
