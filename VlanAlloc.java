package net.floodlightcontroller.vlanalloc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.util.FlowModUtils;
import net.floodlightcontroller.util.OFMessageDamper;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlSrc;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwSrc;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VlanAlloc implements IFloodlightModule, IOFMessageListener {
	
	public static int DEFAULT_HARD_TIMEOUT = 0;
	public static int DEFAULT_IDLE_TIMEOUT = 5;
	public static short DEFAULT_VLANID = 0;  //vlan ID for hosts that doesn't sign in
	public static int DEFAULT_PRIORITY = 60000;
	public static String DEFAULT_GATESW = "00:00:00:00:00:00:00:02";
	public MacAddress DEFAULT_MACGATE = MacAddress.of("00:00:00:00:00:05");
	public MacAddress DEFAULT_BLACK = MacAddress.of("ff:ff:ff:ff:ff:ff");
	public IPv4Address DEFAULT_IPGATE = IPv4Address.of("10.0.0.5");
	public OFPort DEFAULT_PORTGATE = OFPort.of(1);
	public OFPort DEFAULT_PORTWEB = OFPort.of(2);
	
	protected IFloodlightProviderService floodlightProvider;
	protected static Logger logger;
	protected OFMessageDamper messageDamper;

	@Override
	public String getName() {
		return VlanAlloc.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return (type.equals(OFType.PACKET_IN) && name.equals("forwardong"));
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		// eth.getEthType = ??????
		if(eth.getVlanID() == DEFAULT_VLANID){
			//redirect
			boolean online = false; 
			//judge if the host is online
			if(!online){
				switch (msg.getType()) {
				case PACKET_IN:
					return this.directToGate(eth, sw, (OFPacketIn) msg, cntx);
				default:
					break;
				}
			}
		}
		else{
			//add vlan table
			logger.info("Oh my God! host{} has vlan id {}",eth.getSourceMACAddress(),eth.getVlanID());
		}
		return Command.CONTINUE;
	}

	public net.floodlightcontroller.core.IListener.Command directToGate(
			Ethernet eth , IOFSwitch sw, OFPacketIn msg, FloodlightContext cntx) {
		
		OFPacketIn pi = (OFPacketIn) msg;
		OFPort inPort = (pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT));
		MacAddress srcMac = eth.getSourceMACAddress();
		MacAddress dstMac = eth.getDestinationMACAddress();
		
//		if(sw.getId().toString() == "100"){
//			//add flow on sw2 to web server
//			Match.Builder mb_to_web = sw.getOFFactory().buildMatch();
//			mb_to_web.setExact(MatchField.IN_PORT, DEFAULT_PORTGATE);
//			mb_to_web.setExact(MatchField.ETH_DST, DEFAULT_MACGATE);
//			Match m_to_web = mb_to_web.build();
//			OFFlowMod.Builder fmb_to_web = sw.getOFFactory().buildFlowAdd();
//			List<OFAction> actions_to_web = new ArrayList<OFAction>();
//			OFActionOutput.Builder oao_to_web = sw.getOFFactory().actions().buildOutput();
//			oao_to_web.setPort(DEFAULT_PORTWEB);
//			actions_to_web.add(oao_to_web.build());
//			U64 cookie_to_web = AppCookie.makeCookie(2, 0);// cookie ??????
//			fmb_to_web.setCookie(cookie_to_web)
//			.setHardTimeout(DEFAULT_HARD_TIMEOUT)
//			.setIdleTimeout(DEFAULT_IDLE_TIMEOUT)
//			.setBufferId(OFBufferId.NO_BUFFER)
//			.setMatch(m_to_web)
//			.setPriority(DEFAULT_PRIORITY);
//			
//			FlowModUtils.setActions(fmb_to_web, actions_to_web, sw);
//			
//			try{
//				if (logger.isDebugEnabled()) {
//					logger.debug("write redirect flow-mod sw={} match={} flow-mod={}",
//							new Object[] { sw, m_to_web, fmb_to_web.build() });
//				}
//				boolean dampened = messageDamper.write(sw, fmb_to_web.build());
//				logger.debug("OFMessage dampened: {}", dampened);
//			} catch(IOException e){
//				logger.error("Failure writing redirect flow mode", e);
//			}
//			
//			//add flow on sw2 from web server
//			Match.Builder mb_from_web = sw.getOFFactory().buildMatch();
//			mb_from_web.setExact(MatchField.IN_PORT, DEFAULT_PORTWEB);
//			mb_from_web.setExact(MatchField.ETH_SRC, DEFAULT_MACGATE);
//			Match m_from_web = mb_from_web.build();
//			OFFlowMod.Builder fmb_from_web = sw.getOFFactory().buildFlowAdd();
//			List<OFAction> actions_from_web = new ArrayList<OFAction>();
//			OFActionOutput.Builder oao_from_web = sw.getOFFactory().actions().buildOutput();
//			oao_from_web.setPort(DEFAULT_PORTGATE);
//			actions_from_web.add(oao_from_web.build());
//			U64 cookie_from_web = AppCookie.makeCookie(2, 0);// cookie ??????
//			fmb_from_web.setCookie(cookie_from_web)
//			.setHardTimeout(DEFAULT_HARD_TIMEOUT)
//			.setIdleTimeout(DEFAULT_IDLE_TIMEOUT)
//			.setBufferId(OFBufferId.NO_BUFFER)
//			.setMatch(m_from_web)
//			.setPriority(DEFAULT_PRIORITY);
//			
//			FlowModUtils.setActions(fmb_from_web, actions_from_web, sw);
//			
//			try{
//				if (logger.isDebugEnabled()) {
//					logger.debug("write redirect flow-mod sw={} match={} flow-mod={}",
//							new Object[] { sw, m_from_web, fmb_from_web.build() });
//				}
//				boolean dampened = messageDamper.write(sw, fmb_from_web.build());
//				logger.debug("OFMessage dampened: {}", dampened);
//			} catch(IOException e){
//				logger.error("Failure writing redirect flow mode", e);
//			}
//		}
		
		if(!sw.getId().toString().equals(DEFAULT_GATESW)){
//				&& !srcMac.equals(DEFAULT_MACGATE) && !dstMac.equals(DEFAULT_MACGATE) && !srcMac.equals(DEFAULT_BLACK) && !dstMac.equals(DEFAULT_BLACK)){
			//change to gate
			Match.Builder mb = sw.getOFFactory().buildMatch();
			if(eth.getEtherType() == EthType.IPv4){
				IPv4 ip = (IPv4) eth.getPayload();
				IPv4Address srcIP = ip.getSourceAddress();
				IPv4Address dstIP = ip.getDestinationAddress();
				mb.setExact(MatchField.IPV4_SRC, srcIP).setExact(MatchField.IPV4_DST, dstIP);
			}
			
			mb.setExact(MatchField.IN_PORT, inPort);
			mb.setExact(MatchField.ETH_SRC, srcMac).setExact(MatchField.ETH_DST, dstMac);
//			mb.setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofVlan(DEFAULT_VLANID));
			Match m = mb.build();
			
			OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
			List<OFAction> actions = new ArrayList<OFAction>();
			
			OFActionSetDlDst.Builder osd = sw.getOFFactory().actions().buildSetDlDst();
			osd.setDlAddr(DEFAULT_MACGATE);
			actions.add(osd.build());
			
			if(eth.getEtherType() == EthType.IPv4){
				OFActionSetNwDst.Builder osn = sw.getOFFactory().actions().buildSetNwDst();
				osn.setNwAddr(DEFAULT_IPGATE);
				actions.add(osn.build());
			}
			
			OFActionOutput.Builder oao = sw.getOFFactory().actions().buildOutput();
			oao.setPort(DEFAULT_PORTGATE);
			actions.add(oao.build());
			
			U64 cookie = AppCookie.makeCookie(2, 0);// cookie ??????
			fmb.setCookie(cookie)
			.setHardTimeout(DEFAULT_HARD_TIMEOUT)
			.setIdleTimeout(DEFAULT_IDLE_TIMEOUT)
			.setBufferId(OFBufferId.NO_BUFFER)
			.setMatch(m)
			.setPriority(DEFAULT_PRIORITY);
			
			FlowModUtils.setActions(fmb, actions, sw);
			
			try{
				if (logger.isDebugEnabled()) {
					logger.debug("write redirect flow-mod sw={} match={} flow-mod={}",
							new Object[] { sw, m, fmb.build() });
				}
				boolean dampened = messageDamper.write(sw, fmb.build());
				logger.debug("OFMessage dampened: {}", dampened);
			} catch(IOException e){
				logger.error("Failure writing redirect flow mode", e);
			}
			
			//change from gate
			Match.Builder mb_back = sw.getOFFactory().buildMatch();
			List<OFAction> actions_back = new ArrayList<OFAction>();
			if(eth.getEtherType() == EthType.IPv4){
				IPv4 ip = (IPv4) eth.getPayload();
				IPv4Address srcIP = ip.getSourceAddress();
				IPv4Address dstIP = ip.getDestinationAddress();
				mb_back.setExact(MatchField.IPV4_SRC, DEFAULT_IPGATE).setExact(MatchField.IPV4_DST, srcIP);
				OFActionSetNwSrc.Builder ons = sw.getOFFactory().actions().buildSetNwSrc();
				ons.setNwAddr(dstIP);
				actions_back.add(ons.build());
			}
			
			mb_back.setExact(MatchField.ETH_SRC, DEFAULT_MACGATE).setExact(MatchField.ETH_DST, srcMac);
//			mb_back.setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofVlan(DEFAULT_VLANID));
			Match m_back = mb_back.build();
			
			OFActionSetDlSrc.Builder oss = sw.getOFFactory().actions().buildSetDlSrc();
			oss.setDlAddr(dstMac);
			actions_back.add(oss.build());
			
			OFActionOutput.Builder oao_back = sw.getOFFactory().actions().buildOutput();
			oao_back.setPort(inPort);
			actions_back.add(oao_back.build());
			
			U64 cookie_back = AppCookie.makeCookie(2, 0);// cookie ??????
			fmb.setCookie(cookie_back)
			.setHardTimeout(DEFAULT_HARD_TIMEOUT)
			.setIdleTimeout(DEFAULT_IDLE_TIMEOUT)
			.setBufferId(OFBufferId.NO_BUFFER)
			.setMatch(m_back)
			.setPriority(DEFAULT_PRIORITY);
			
			FlowModUtils.setActions(fmb, actions_back, sw);
			
			try{
				if (logger.isDebugEnabled()) {
					logger.debug("write redirect flow-mod sw={} match={} flow-mod={}",
							new Object[] { sw, m_back, fmb.build() });
				}
				boolean dampened = messageDamper.write(sw, fmb.build());
				logger.debug("OFMessage dampened: {}", dampened);
			} catch(IOException e){
				logger.error("Failure writing redirect flow mode", e);
			}
			
			//deal with the packet
			OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
			List<OFAction> actions_out = new ArrayList<OFAction>();
			actions_out.add(sw.getOFFactory().actions().setDlDst(DEFAULT_MACGATE));
			actions_out.add(sw.getOFFactory().actions().setNwDst(DEFAULT_IPGATE));
			pob.setActions(actions_out);

			pob.setBufferId(OFBufferId.NO_BUFFER);
			byte[] packetData = pi.getData();
			pob.setData(packetData);

			pob.setInPort((pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT)));

			try {
				messageDamper.write(sw, pob.build());
			} catch (IOException e) {
				logger.error("Failure writing packet out", e);
			}
		}
		return Command.CONTINUE;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		logger = LoggerFactory.getLogger(VlanAlloc.class);
		messageDamper = new OFMessageDamper(10000,
				EnumSet.of(OFType.FLOW_MOD),
				250);
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}

}
