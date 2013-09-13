package com.aws.securitygroups;

import java.io.InputStream;
import java.util.Properties;

import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;

public class LoadConfigUtility {
	private static Properties properties = new Properties();
	private static AmazonEC2 amazonEC2 = new AmazonEC2Client(new ClasspathPropertiesFileCredentialsProvider());
	private static Region region;
	
	static{
		try{
			InputStream propFileInputStream = LoadConfigUtility.class.getClassLoader().getResourceAsStream("config.properties");
			properties.load(propFileInputStream);			
			region = RegionUtils.getRegion(properties.getProperty("request.region"));
			amazonEC2.setRegion(region);
		}catch(Exception e){
			e.printStackTrace();
		}	
	}
	
	public static Properties getProperties() {
		return properties;
	}

	public static AmazonEC2 getAmazonEC2() {
		return amazonEC2;
	}

	public static Region getRegion() {
		return region;
	}	
	
}
