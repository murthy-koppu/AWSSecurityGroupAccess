package com.aws.securitygroups;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class FetchSecurityGroupOfInstances {
	
	/*
	 * Result O/P
	 * {
  "SGroupAssociatedToInstance": [
    {
      "AssociatedInstances": [
        "i-1ea55272",
        "i-e82afb80"
      ],
      "RestrictedIPs": [
        {
          "IPAdrress": [],
          "IPProtocol": "tcp",
          "PortRange": "0..65535"
        },
        {
          "IPAdrress": [],
          "IPProtocol": "udp",
          "PortRange": "0..65535"
        },
        {
          "IPAdrress": [],
          "IPProtocol": "icmp"
        }
      ],
      "SecurityGroupName": "default"
    },
    {
      "AssociatedInstances": "i-1ea55272",
      "RestrictedIPs": {
        "IPAdrress": ["0.0.0.0/0"],
        "IPProtocol": "tcp",
        "PortRange": "22"
      },
      "SecurityGroupName": "quicklaunch-1"
    }
  ],
  "SGroupNonAssociated": {
    "RestrictedIPs": {
      "IPAdrress": ["0.0.0.0/0"],
      "IPProtocol": "tcp",
      "PortRange": "995"
    },
    "SecurityGroupName": "SecurityGrp-2"
  }
}

	 */
	
	public static void main(String[] args) {
		FetchSecurityGroupOfInstances obj = new FetchSecurityGroupOfInstances();		
		try {
			System.out.println(obj.getJsonOfSGs()==null?"No SecurityGroups present ":obj.getJsonOfSGs().toString(2));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}




	private  JSONObject getJsonOfSGs() {		
		List<Instance> lsInstances = getLsEC2Instance();
		Map<String,List<Instance>> securityGroupIdentifiers = new HashMap<String,List<Instance>>();
		for(Instance instance: lsInstances){
			for(GroupIdentifier groupIdentifier:instance.getSecurityGroups()){
				List<Instance> sgInstances = securityGroupIdentifiers.get(groupIdentifier.getGroupId());
				if(sgInstances == null){
					sgInstances = new ArrayList<Instance>(lsInstances.size());
					securityGroupIdentifiers.put(groupIdentifier.getGroupId(), sgInstances);
				}
				sgInstances.add(instance);
			}
		}
		
		return publishAllSecurityGroupDetailsAsJSONWithInstanceId(securityGroupIdentifiers);
		
	}
	
	
	
	
	private List<Instance> getLsEC2Instance(){
		List<Instance> lsInstances = new ArrayList<Instance>();
		DescribeInstancesResult instancesResult = LoadConfigUtility.getAmazonEC2().describeInstances();
		if(instancesResult != null && instancesResult.getReservations() != null){
			for(Reservation reservation: instancesResult.getReservations()){
				lsInstances.addAll(reservation.getInstances());
			}
		}
		return lsInstances;
	}
	
	private JSONObject publishAllSecurityGroupDetailsAsJSONWithInstanceId(Map<String,List<Instance>> groupIdentifiersWithInstances){
		DescribeSecurityGroupsResult securityGroupRslt =  LoadConfigUtility.getAmazonEC2().describeSecurityGroups();
		if(securityGroupRslt != null && securityGroupRslt.getSecurityGroups() != null && !securityGroupRslt.getSecurityGroups().isEmpty()){
			JSONObject jsonSGsParent = new JSONObject();
			for(SecurityGroup securityGroup : securityGroupRslt.getSecurityGroups()){
				try {
					JSONObject jsonSG = getJSONSGFromSecurityGroupObj(securityGroup);
					if(groupIdentifiersWithInstances.containsKey(securityGroup.getGroupId())){
						List<Instance> instances = groupIdentifiersWithInstances.get(securityGroup.getGroupId());
						for(Instance instance:instances){
							jsonSG.accumulate("AssociatedInstances", instance.getInstanceId());
						}
						jsonSGsParent.accumulate("SGroupAssociatedToInstance", jsonSG);						
					}else
						jsonSGsParent.accumulate("SGroupNonAssociated", jsonSG);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return jsonSGsParent;		
		}
		return null;
	}
	
	private JSONObject publishSecurityGroupDetailsAsJSON(Set<String> groupIdentifiers){
		DescribeSecurityGroupsRequest securityGroupReq = new DescribeSecurityGroupsRequest();
		securityGroupReq.setGroupIds(groupIdentifiers);
		DescribeSecurityGroupsResult securityGroupRslt =  LoadConfigUtility.getAmazonEC2().describeSecurityGroups(securityGroupReq);
		if(securityGroupRslt != null && securityGroupRslt.getSecurityGroups() != null && !securityGroupRslt.getSecurityGroups().isEmpty()){
			JSONObject jsonSGsParent = new JSONObject();
			for(SecurityGroup securityGroup : securityGroupRslt.getSecurityGroups()){
				try {
					jsonSGsParent.accumulate("SecurityGroups", getJSONSGFromSecurityGroupObj(securityGroup));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			return jsonSGsParent;
		}
		return null;
	}

	private JSONObject getJSONSGFromSecurityGroupObj(
			SecurityGroup securityGroup) {
			JSONObject jsonSG = new JSONObject();
				try{					
					jsonSG.put("SecurityGroupName", securityGroup.getGroupName());
					for(IpPermission ipPermission : securityGroup.getIpPermissions()){
						JSONObject jsonObjIPPerm = new JSONObject();
						setPortToJSON(ipPermission.getFromPort(), ipPermission.getToPort(), jsonObjIPPerm);
						jsonObjIPPerm.put("IPProtocol", ipPermission.getIpProtocol());
						jsonObjIPPerm.put("IPAdrress",ipPermission.getIpRanges());
						jsonSG.accumulate("RestrictedIPs", jsonObjIPPerm);
					}
				}catch (JSONException e) {
					e.printStackTrace();
				}catch(Exception e){
					e.printStackTrace();
				}		
		return jsonSG;
	}
	
	private void setPortToJSON(Integer fromPort, Integer toPort, JSONObject jsonObjPort){
		if(toPort.equals(-1)){
			return;
		} 
		try{
			if(fromPort.equals(toPort)){
				jsonObjPort.put("PortRange", toPort.toString());
			}else{
				jsonObjPort.put("PortRange", fromPort+".."+toPort);
			}
		}catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
}

