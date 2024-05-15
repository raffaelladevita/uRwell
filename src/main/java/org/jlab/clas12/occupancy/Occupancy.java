package org.jlab.clas12.occupancy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.JFrame;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.group.DataGroup;
import org.jlab.groot.base.GStyle;
import org.jlab.groot.graphics.EmbeddedCanvasTabbed;
import org.jlab.groot.graphics.EmbeddedPad;
import org.jlab.groot.ui.TCanvas;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.*;
import org.jlab.logging.DefaultLogger;
import org.jlab.utils.benchmark.ProgressPrintout;
import org.jlab.utils.options.OptionParser;
        
        
        /**
 *
 * @author devita
 */
public class Occupancy {
    
    Map<String, DataGroup> dgs = new LinkedHashMap<>();
    
    double current = 50;
    String[] titles = {"hits", "Cluster", "Track"};

    Random rand = new Random();
    
    public Occupancy() {
        this.createHistos();
    }
    
    
    
    
    public final void createHistos() {

        GStyle.getAxisAttributesX().setTitleFontSize(18); //24
        GStyle.getAxisAttributesX().setLabelFontSize(18); //18
        GStyle.getAxisAttributesY().setTitleFontSize(18); //24
        GStyle.getAxisAttributesY().setLabelFontSize(18); //18
        GStyle.getAxisAttributesZ().setLabelFontSize(14); //14
        GStyle.setPalette("kDefault");
        GStyle.getAxisAttributesX().setLabelFontName("Arial");
        GStyle.getAxisAttributesY().setLabelFontName("Arial");
        GStyle.getAxisAttributesZ().setLabelFontName("Arial");
        GStyle.getAxisAttributesX().setTitleFontName("Arial");
        GStyle.getAxisAttributesY().setTitleFontName("Arial");
        GStyle.getAxisAttributesZ().setTitleFontName("Arial");
        GStyle.setGraphicsFrameLineWidth(1);
        GStyle.getH1FAttributes().setLineWidth(2);
        GStyle.getH1FAttributes().setOptStat("1111");

        DataGroup dgDC = new DataGroup(3, 4);
        for(int i=0; i<titles.length; i++) {
            String title = titles[i];
            H2F hi_occ2D = new H2F("hiOcc2D"+title, "Occupancy for hits on "+title, 112, 1, 113, 36, 1, 37 );  
            if(i==0) hi_occ2D.setTitle("Occupancy"); 
            hi_occ2D.setTitleX("Wire"); 
            hi_occ2D.setTitleY("Layer"); 
            H1F hi_occ1D = new H1F("hiOcc1D"+title, "Occupancy for hits on "+title, 3, 1, 4);  
            hi_occ1D.setTitleX("Region"); 
            hi_occ1D.setTitleY("Occupancy (%)");
            hi_occ1D.setLineColor(i+1);
            H1F hi_mult = new H1F("hiMult"+title, title+"Multiplicity", 150, 0, 1500);  
            hi_mult.setTitleX("Multiplicity"); 
            hi_mult.setTitleY("Counts"); 
            hi_mult.setLineColor(i+1);
            H1F hi_norm = new H1F("hiNorm"+title, title+"Norm", 100, 0, 10);  
            hi_norm.setTitleX("Occupancy (%)"); 
            hi_norm.setTitleY("Counts");
            hi_norm.setLineColor(i+1);
            H1F hi_enrg = new H1F("hiEnergy"+title, title+"Energy", 100, 0, 1000);  
            hi_enrg.setTitleX("Energy (eV)"); 
            hi_enrg.setTitleY("Counts");
            H1F hi_vrtx = new H1F("hiVertex"+i,"Vertex", 100, -100, 200);  
            hi_vrtx.setTitleX("Energy (eV)"); 
            hi_vrtx.setTitleY("Counts"); 
            dgDC.addDataSet(hi_occ2D, i);
            dgDC.addDataSet(hi_occ1D, 3);
            dgDC.addDataSet(hi_mult,  4);
            dgDC.addDataSet(hi_norm,  5);
            dgDC.addDataSet(hi_enrg,  9);
            if(i>0) dgDC.addDataSet(hi_vrtx,  10);

            for(int iregion=0; iregion<3; iregion++) {
                int region = iregion+1;
                H1F hi_tdc = new H1F("hiTDC"+title+region, "Region " + region, 150, 0, 1500);  
                hi_tdc.setTitleX("TDC"); 
                hi_tdc.setTitleY("Counts");
                hi_tdc.setLineColor(i+1);
                dgDC.addDataSet(hi_tdc, 6 + iregion);
            }
        }
        dgs.put("DC", dgDC);
        
        DataGroup dgURW = new DataGroup(3,2);
        H2F hi_occ2D = new H2F("hiOcc2D", "Occupancy", 2000, 1, 2001, 2, 1, 3);  
        hi_occ2D.setTitleX("Strip"); 
        hi_occ2D.setTitleY("Layer"); 
        H1F hi_mult = new H1F("hiMult", "Multiplicity", 150, 0, 1000);  
        hi_mult.setTitleX("Multiplicity"); 
        hi_mult.setTitleY("Counts"); 
        H1F hi_norm = new H1F("hiNorm", "Norm", 100, 0, 5);  
        hi_norm.setTitleX("Occupancy (%)"); 
        hi_norm.setTitleY("Counts");
        dgURW.addDataSet(hi_occ2D, 0);
        dgURW.addDataSet(hi_mult,  1);
        dgURW.addDataSet(hi_norm,  2);
        for(int ilayer=0; ilayer<3; ilayer++) {
            int layer = ilayer+1;            
            H1F hi_enrg = new H1F("hiEnergy"+layer, "Energy", 100, 0, 1000);  
            hi_enrg.setTitleX("Energy (eV)"); 
            hi_enrg.setTitleY("Counts");
            hi_enrg.setLineColor(layer);
            H1F hi_time = new H1F("hiTime"+layer, "Time", 100, 0, 1000);  
            hi_time.setTitleX("Time (ns)"); 
            hi_time.setTitleY("Counts");
            hi_time.setLineColor(layer);
            
            dgURW.addDataSet(hi_enrg, 3);           
            dgURW.addDataSet(hi_time, 4);           
        }
        dgs.put("URWELL", dgURW);
    }
    
    
    public void processEvent(DataEvent event) {
        DataBank dcTDC = null;
        DataBank hbHit = null;
        DataBank hbTHit = null;
        DataBank hbTrack = null;
        DataBank urwADC = null;
        DataBank mcTrue = null;

        if (!event.hasBank("RUN::config")) {
            return;
        }
        int run = event.getBank("RUN::config").getInt("run", 0);

        if (event.hasBank("MC::True")) {
            mcTrue = event.getBank("MC::True");
        }
        if (event.hasBank("DC::tdc")) {
            dcTDC = event.getBank("DC::tdc");
        }
        if (event.hasBank("HitBasedTrkg::Hits")) {
            hbHit = event.getBank("HitBasedTrkg::Hits");
        }
        if (event.hasBank("HitBasedTrkg::HBHits")) {
            hbTHit = event.getBank("HitBasedTrkg::HBHits");
        }
        if (event.hasBank("HitBasedTrkg::HBTracks")) {
            hbTrack = event.getBank("HitBasedTrkg::HBTracks");
        }
        if (event.hasBank("URWELL::adc")) {
            urwADC = event.getBank("URWELL::adc");
        }

        if (dcTDC != null) {
            for (int i = 0; i < dcTDC.rows(); i++) {
                int sector = dcTDC.getByte("sector", i);
                int layer = dcTDC.getByte("layer", i);
                int wire = dcTDC.getShort("component", i);
                int order = dcTDC.getByte("order", i);
                int tdc = dcTDC.getInt("TDC", i);
                int region = (layer - 1) / 12 + 1;
                float edep = 0;
                if (mcTrue != null) {
                    edep = mcTrue.getFloat("totEdep", i);
                }
                dgs.get("DC").getH2F("hiOcc2Dhits").fill(wire, layer);
                dgs.get("DC").getH1F("hiOcc1Dhits").fill(region);
                dgs.get("DC").getH1F("hiTDChits" + region).fill(tdc);
                dgs.get("DC").getH1F("hiEnergyhits").fill(edep * 1E6);
            }
            dgs.get("DC").getH1F("hiMulthits").fill(dcTDC.rows());
            dgs.get("DC").getH1F("hiNormhits").fill(100.0 * dcTDC.rows() / 6 / 36 / 112);
        }
        if (hbHit != null) {
            int nHitClus = 0;
            for (int i = 0; i < hbHit.rows(); i++) {
                int sector = hbHit.getByte("sector", i);
                int suplay = hbHit.getByte("superlayer", i);
                int layer = hbHit.getByte("layer", i);
                int wire = hbHit.getShort("wire", i);
                int clusID = hbHit.getShort("clusterID", i);
                int tdc = hbHit.getInt("TDC", i);
                int region = (suplay - 1) / 2 + 1;
                if (clusID > 0) {
                    dgs.get("DC").getH2F("hiOcc2DCluster").fill(wire, layer + 6 * (suplay - 1));
                    dgs.get("DC").getH1F("hiOcc1DCluster").fill(region);
                    dgs.get("DC").getH1F("hiTDCCluster" + region).fill(tdc);
                    nHitClus++;
                }
            }
            dgs.get("DC").getH1F("hiMultCluster").fill(nHitClus);
            dgs.get("DC").getH1F("hiNormCluster").fill(100.0 * nHitClus / 6 / 36 / 112);
        }
        if (hbTHit != null) {
            for (int i = 0; i < hbTHit.rows(); i++) {
                int sector = hbTHit.getByte("sector", i);
                int suplay = hbTHit.getByte("superlayer", i);
                int layer = hbTHit.getByte("layer", i);
                int wire = hbTHit.getShort("wire", i);
                int clusID = hbTHit.getShort("clusterID", i);
                int tdc = hbTHit.getInt("TDC", i);
                int region = (suplay - 1) / 2 + 1;
                dgs.get("DC").getH2F("hiOcc2DTrack").fill(wire, layer + 6 * (suplay - 1));
                dgs.get("DC").getH1F("hiOcc1DTrack").fill(region);
                dgs.get("DC").getH1F("hiTDCTrack" + region).fill(tdc);
            }
            dgs.get("DC").getH1F("hiMultTrack").fill(hbTHit.rows());
            dgs.get("DC").getH1F("hiNormTrack").fill(100.0 * hbTHit.rows() / 6 / 36 / 112);
        }
        if (hbTrack != null) {
            for (int i = 0; i < hbTrack.rows(); i++) {
                double z = hbTrack.getFloat("Vtx0_z", i);
                double d = rand.nextGaussian();
                double zp = Math.sqrt(z*z+d*d);
                dgs.get("DC").getH1F("hiVertex1").fill(zp);
                if (z > 30) {
                    dgs.get("DC").getH1F("hiVertex2").fill(z);
                }
            }
        }
        
      
        if (urwADC != null) {
            for(int i=0; i<urwADC.rows(); i++) {
                int    sector = urwADC.getByte("sector", i);
                int    layer  = urwADC.getByte("layer", i);
                int    strip  = urwADC.getShort("component", i);
                int    adc    = urwADC.getInt("ADC", i);
                double time   = urwADC.getFloat("time", i);

                dgs.get("URWELL").getH2F("hiOcc2D").fill(strip, layer);
                dgs.get("URWELL").getH1F("hiEnergy"+layer).fill(adc);
                dgs.get("URWELL").getH1F("hiTime"+layer).fill(time);
            }
            dgs.get("URWELL").getH1F("hiMult").fill(urwADC.rows());
            dgs.get("URWELL").getH1F("hiNorm").fill(100.0 * urwADC.rows() / 6 / 2 / 1850);
        }

    }
    
    
    public void analyzeHisto(int nevents) {
        
        nevents *= current/50;

        double norm = nevents*6/100;
        for(int i=0; i<titles.length; i++) {
            String title = titles[i];

            H2F occ = dgs.get("DC").getH2F("hiOcc2D"+title);
            for(int loop = 0; loop < occ.getDataBufferSize(); loop++){
                double r = occ.getDataBufferBin(loop);
                if(r>0) occ.setDataBufferBin(loop,r/norm);
            }

            dgs.get("DC").getH1F("hiOcc1D"+title).divide((double) nevents*6*12*112/100);

            for(int iregion=0; iregion<3; iregion++) {
                int region = iregion +1;
                dgs.get("DC").getH1F("hiTDC" + title + region).divide((double) nevents);
            }
        }
        H2F occ = dgs.get("URWELL").getH2F("hiOcc2D");
        for(int loop = 0; loop < occ.getDataBufferSize(); loop++){
            double r = occ.getDataBufferBin(loop);
            if(r>0) occ.setDataBufferBin(loop,r/norm);
        }
    }
    
        
    public EmbeddedCanvasTabbed getCanvas() {
        EmbeddedCanvasTabbed canvas = null;
        for(String key : dgs.keySet()) {
            if(canvas == null)
                canvas = new EmbeddedCanvasTabbed(key);
            else
                canvas.addCanvas(key);
            canvas.getCanvas(key).draw(dgs.get(key));
        }
        return canvas;
    }
    
    public static void main(String[] args) {
        

        OptionParser parser = new OptionParser("urwell");
        parser.setRequiresInputList(false);
        parser.addOption("-n","-1", "maximum number of events to process");
        parser.addOption("-w", "1", "open graphical window (1) or run in batch mode (0)");
        parser.parse(args);
        
        
        int     maxEvents = parser.getOption("-n").intValue();
        boolean window    = parser.getOption("-w").intValue()==1;
        
        if(!window) System.setProperty("java.awt.headless", "true");
        DefaultLogger.debug();
        
        Occupancy analysis = new Occupancy();        

        List<String> inputFiles = parser.getInputList();
        
        ProgressPrintout progress = new ProgressPrintout();

        int counter=-1;

        for(String input : inputFiles) {
            HipoDataSource  reader = new HipoDataSource();
            reader.open(input);
            while(reader.hasEvent()) {
                
                counter++;
                
                DataEvent ev = reader.getNextEvent();

                analysis.processEvent(ev);
                
                progress.updateStatus();
                
                if(maxEvents>0){
                    if(counter>=maxEvents) break;
                }

            }
            progress.showStatus();
            reader.close();
        }   
        analysis.analyzeHisto(counter);
    
        if(window) {
            JFrame frame = new JFrame("URWell Reconstruction");
            frame.setSize(1500,1000);
            frame.add(analysis.getCanvas());
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);     
        }
    }
}



