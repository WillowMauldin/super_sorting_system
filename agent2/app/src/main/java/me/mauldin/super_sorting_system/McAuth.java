package me.mauldin.super_sorting_system;

import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import net.raphimc.minecraftauth.step.java.StepMCToken;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession.FullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepCredentialsMsaCode;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class McAuth {
	public static FullJavaSession getSession() throws Exception {
		HttpClient httpClient = MinecraftAuth.createHttpClient();

		// Attempt to load existing credentials
		File authFile = new File("auth.json");
		if (authFile.exists()) {
			FileReader reader = new FileReader(authFile);
			JsonObject serializedSession = JsonParser.parseReader(reader).getAsJsonObject();
			FullJavaSession loadedSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.fromJson(serializedSession);
			System.out.println("Loaded existing session for: " + loadedSession.getMcProfile().getName());
			FullJavaSession readyToUseSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.refresh(httpClient, loadedSession);
			return readyToUseSession;
		}

		// Get new login
		System.out.println("No valid session found, starting new login...");
		FullJavaSession javaSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.getFromInput(httpClient, new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCode -> {
		    System.out.println("Go to " + msaDeviceCode.getDirectVerificationUri());
		}));

		// Save credentials after successful login
		JsonObject serializedSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.toJson(javaSession);
		FileWriter writer = new FileWriter(authFile);
		writer.write(serializedSession.toString());
		writer.flush();

		return javaSession;
	}
}
