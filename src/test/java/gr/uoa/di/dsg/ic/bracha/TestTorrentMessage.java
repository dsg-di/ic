package gr.uoa.di.dsg.ic.bracha;

import java.io.File;
import java.io.IOException;

import com.google.common.io.Files;


import org.junit.Test;

import gr.uoa.di.dsg.ic.torrent.ICTorrentMessage;

public class TestTorrentMessage {
	
	@Test
	public void test() throws IOException {
		ICTorrentMessage tor = new ICTorrentMessage("1","test.com","10/10/10","/media/mkonstant/Data/DSG/test/files/node8.torrent" );
		
		byte[] data = tor.serialize();
		//System.out.println(data);
		
		ICTorrentMessage test = ICTorrentMessage.deserialize(data);
		//System.out.println(test.getCrawlSite());
		
		
		Files.write(test.getTorrentData() , new File("/media/mkonstant/Data/DSG/test/files/test.torrent"));
	}

}
