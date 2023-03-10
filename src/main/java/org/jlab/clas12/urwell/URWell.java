package org.jlab.clas12.urwell;

import cnuphys.magfield.MagneticFieldInitializationException;
import cnuphys.magfield.MagneticFields;
import eu.mihosoft.vrl.v3d.Vector3d;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.utils.options.OptionParser;
import org.jlab.detector.base.DetectorType;
import org.jlab.geom.prim.Point3D;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.math.F1D;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.group.DataGroup;
import javax.swing.JFrame;
import org.jlab.clas.swimtools.Swim;
import org.jlab.clas12.urwell.Event.Cluster;
import org.jlab.clas12.urwell.Event.Cross;
import org.jlab.clas12.urwell.Event.Hit;
import org.jlab.clas12.urwell.Event.DCCluster;
import org.jlab.clas12.urwell.Event.DCHit;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.detector.geant4.v2.DCGeant4Factory;
import org.jlab.geom.base.ConstantProvider;
import org.jlab.geom.prim.Line3D;
import org.jlab.geom.prim.Transformation3D;
import org.jlab.geom.prim.Vector3D;
import org.jlab.groot.graphics.EmbeddedCanvasTabbed;
import org.jlab.groot.graphics.EmbeddedPad;
import org.jlab.io.hipo.HipoDataSync;
import org.jlab.logging.DefaultLogger;
import org.jlab.utils.CLASResources;
import org.jlab.utils.benchmark.ProgressPrintout;

/**
 *
 * @author devita
 */
public class URWell {
    
    public final static int NSECTOR  = 6;
    public final static int NLAYER   = 2;
    public final static int NCHAMBER = 3;

    private final static Line3D[][] dcWires = new Line3D[36][112];
    private final static Vector3D[] dcNorms = new Vector3D[6];
    public final static Transformation3D[] TOLOCAL = new Transformation3D[6];
    public final static double TILT = 25;
    
    private Swim swim = null;
    
    private final Map<String, DataGroup> dataGroups = new LinkedHashMap<>();

    private static final String[] axes = {"x", "y"};
    
    private Point3D[] trajs = new Point3D[NLAYER];

    public URWell() {
        this.createHistos();
        this.loadFields();
        this.loadGeometry();
    }
    
    public EmbeddedCanvasTabbed getCanvas() {
        EmbeddedCanvasTabbed canvas = null;
        for(String key : dataGroups.keySet()) {
            if(canvas == null)
                canvas = new EmbeddedCanvasTabbed(key);
            else
                canvas.addCanvas(key);
            canvas.getCanvas(key).draw(dataGroups.get(key));
            for(EmbeddedPad pad :canvas.getCanvas(key).getCanvasPads()) {
                pad.getAxisZ().setLog(true);
            }
        }
        canvas.getCanvas("Matching").getPad(2).getAxisZ().setLog(true);
        canvas.getCanvas("Matching").getPad(3).getAxisZ().setLog(true);
        canvas.getCanvas("Matching").getPad(7).getAxisZ().setLog(true);
        canvas.getCanvas("Matching").getPad(8).getAxisZ().setLog(true);
        return canvas;
    }
    
    public DataGroup getGroup(String key) {
        return this.dataGroups.get(key);
    }
    
    private void loadFields() {
        try {
            String clasDictionaryPath = CLASResources.getResourcePath("etc");
            String magfieldDir = clasDictionaryPath + "/data/magfield/";
            String solenoidMap = "Symm_solenoid_r601_phi1_z1201_13June2018.dat";
            String torusMap    = "Symm_torus_r2501_phi16_z251_24Apr2018.dat";
            MagneticFields.getInstance().initializeMagneticFields(magfieldDir,torusMap,solenoidMap);
            // scale and shift field for RG-A/B inbending configuration
            MagneticFields.getInstance().getTorus().setScaleFactor(-1);
            MagneticFields.getInstance().getSolenoid().setScaleFactor(-1);
            MagneticFields.getInstance().getSolenoid().setShiftZ(-3);
        } catch (FileNotFoundException | MagneticFieldInitializationException ex) {
            Logger.getLogger(URWell.class.getName()).log(Level.SEVERE, null, ex);
        }
        swim = new Swim();
    }
    
    private void loadGeometry() {
        ConstantProvider provider   = GeometryFactory.getConstants(DetectorType.DC, 11, "default");
        DCGeant4Factory  dcDetector = new DCGeant4Factory(provider, DCGeant4Factory.MINISTAGGERON, false);
        for(int is=0; is<6; is++) {
            dcNorms[is] = dcDetector.getTrajectorySurface(is+1, 1, 1).normal();
            TOLOCAL[is] = new Transformation3D(); 
            TOLOCAL[is].rotateZ(-is*Math.toRadians(60));
            TOLOCAL[is].rotateY(-Math.toRadians(TILT));
        }

        for(int il=0; il<36; il++) {
            for(int iw=0; iw<112; iw++) {
                int isl=il/6;
                int ily=il%6;
                Vector3d left  = dcDetector.getWireLeftend(0,isl,ily,iw);
                Vector3d right = dcDetector.getWireRightend(0,isl,ily,iw);
                dcWires[il][iw] = new Line3D(left.x,left.y,left.z,right.x,right.y,right.z);
        //	if(isl%2==0) wires[il][iw].rotateZ(Math.toRadians(6));
        //	else         wires[il][iw].rotateZ(Math.toRadians(-6));
            }
        }
    }
    
    private void createHistos() {
        
        DataGroup hits = new DataGroup(5,2);
        for(int il=0; il<NLAYER; il++) {
            int layer = il+1;
            H1F hit1 = new H1F("hiEnergyL"+layer, "Energy (eV)", "Counts", 100, 0.0, 1500.0);         
            hit1.setOptStat(Integer.parseInt("1111")); 
            H1F hit2 = new H1F("hiTimeL"+layer, "Time (ns)", "Counts", 100, 0.0, 600.0);         
            hit2.setOptStat(Integer.parseInt("1111")); 
            H2F hit3 = new H2F("hiEnergyStripL"+layer, "", 100, 0.0, 1500.0, 2000, 0, 2000);         
            hit3.setTitleX("Energy (ev)");
            hit3.setTitleY("Strip");
            H2F hit4 = new H2F("hiTimeStripL"+layer, "", 100, 0.0, 600.0, 2000, 0, 2000);         
            hit4.setTitleX("Time (ns)");
            hit4.setTitleY("Strip");
            H2F hit5 = new H2F("hiTimeEnergyL"+layer, "", 100, 0.0, 600.0, 100, 0, 1500);         
            hit5.setTitleX("Time (ns)");
            hit5.setTitleY("Strip");
            hits.addDataSet(hit1,  il*5 + 0);
            hits.addDataSet(hit2,  il*5 + 1);
            hits.addDataSet(hit3,  il*5 + 2);
            hits.addDataSet(hit4,  il*5 + 3);
            hits.addDataSet(hit5,  il*5 + 4);
        }
        dataGroups.put("Hits", hits);

        DataGroup clusters = new DataGroup(5,2);
        for(int il=0; il<NLAYER; il++) {
            int layer = il+1;
            H1F cluster1 = new H1F("hiEnergyL"+layer, "Energy (eV)", "Counts", 100, 0.0, 1500.0);         
            cluster1.setOptStat(Integer.parseInt("1111")); 
            H1F cluster2 = new H1F("hiTimeL"+layer, "Time (ns)", "Counts", 100, 0.0, 600.0);         
            cluster2.setOptStat(Integer.parseInt("1111")); 
            H2F cluster3 = new H2F("hiEnergyStripL"+layer, "", 100, 0.0, 1500.0, 2000, 0, 2000);         
            cluster3.setTitleX("Energy (ev)");
            cluster3.setTitleY("Strip");
            H2F cluster4 = new H2F("hiTimeStripL"+layer, "", 100, 0.0, 600.0, 2000, 0, 2000);         
            cluster4.setTitleX("Time (ns)");
            cluster4.setTitleY("Strip");
            H1F cluster5 = new H1F("hiDistanceL"+layer, "Distance (mm)", "Counts", 100, -2, 2);         
            cluster5.setOptStat("1111");
            clusters.addDataSet(cluster1,  il*5 + 0);
            clusters.addDataSet(cluster2,  il*5 + 1);
            clusters.addDataSet(cluster3,  il*5 + 2);
            clusters.addDataSet(cluster4,  il*5 + 3);
            clusters.addDataSet(cluster5,  il*5 + 4);
        }
        dataGroups.put("Clusters", clusters);

        DataGroup crosses = new DataGroup(4,2);
        H1F cross1 = new H1F("hiEnergy", "Energy (eV)", "Counts", 100, 0.0, 1500.0);         
        cross1.setOptStat(Integer.parseInt("1111")); 
        H1F cross1cut = new H1F("hiEnergyGood", "Energy (eV)", "Counts", 100, 0.0, 1500.0);         
        cross1cut.setOptStat(Integer.parseInt("1111")); 
        cross1cut.setLineColor(2);
        H2F cross2 = new H2F("hiEnergyX", "", 100, 0.0, 1500.0, 100, -100, 100);         
        cross2.setTitleX("Energy (eV)");
        cross2.setTitleY("x (cm)");
        H2F cross3 = new H2F("hiEnergy2D", "", 100, 0.0, 1500.0, 100, 0.0, 1500.);         
        cross3.setTitleX("Energy1 (eV)");
        cross3.setTitleY("Energy2 (eV)");
        H1F cross4 = new H1F("hiDeltaX", "Cross #DeltaX (mm)", "Counts", 100, -20.0, 20.0);         
        cross4.setOptStat(Integer.parseInt("1111")); 
        H1F cross5 = new H1F("hiTime", "Time (ns)", "Counts", 100, 0.0, 600.0);         
        cross5.setOptStat(Integer.parseInt("1111")); 
        H1F cross5cut = new H1F("hiTimeGood", "Time (ns)", "Counts", 100, 0.0, 600.0);         
        cross5cut.setOptStat(Integer.parseInt("1111")); 
        cross5cut.setLineColor(2);
        H2F cross6 = new H2F("hiTimeX", "", 100, 0.0, 600.0,  100, -100, 100);        
        cross6.setTitleX("Time (ns)");
        cross6.setTitleY("x (mm)");
        H2F cross7 = new H2F("hiTime2D", "", 100, 0.0, 600.0,  100, 0, 600);        
        cross7.setTitleX("Time1 (ns)");
        cross7.setTitleY("Time2 (ns)");
        H1F cross8 = new H1F("hiDeltaY", "Cross #DeltaY (mm)", "Counts", 100, -20.0, 20.0);         
        cross8.setOptStat(Integer.parseInt("1111")); 
        crosses.addDataSet(cross1,    0);
        crosses.addDataSet(cross1cut, 0);
        crosses.addDataSet(cross2,    1);
        crosses.addDataSet(cross3,    2);
        crosses.addDataSet(cross4,    3);
        crosses.addDataSet(cross5,    4);
        crosses.addDataSet(cross5cut, 4);
        crosses.addDataSet(cross6,    5);
        crosses.addDataSet(cross7,    6);
        crosses.addDataSet(cross8,    7);
        dataGroups.put("Crosses", crosses);

        DataGroup dg = new DataGroup(5, 2);
        for(int il=0; il<NLAYER; il++) {
            int layer = il+1;
            H1F h1 = new H1F("hiEnergyL"+layer, "Cluster Energy (eV)", "Counts", 100, 0.0, 1500.0);         
            h1.setOptStat(Integer.parseInt("1111")); 
            H1F h2 = new H1F("hiTimeL"+layer, "Cluster Time (ns)", "Counts", 100, 0.0, 600.0);         
            h2.setOptStat(Integer.parseInt("1111")); 
            H1F h2c = new H1F("hiTimeCutL"+layer, "Hit Time (ns)", "Counts", 100, 0.0, 600.0);         
            h2c.setOptStat(Integer.parseInt("1111")); 
            h2c.setLineColor(2);
            H2F h3 = new H2F("hiDCL"+layer, "", 100, -100, 100, 56, 0, 112);
            h3.setTitleX("uRWell cross x (mm)");
            h3.setTitleY("DC SL" + layer + " cluster average wire");
            H2F h3c = new H2F("hiDCCutL"+layer, "", 100, -100, 100, 56, 0, 112);
            h3c.setTitleX("uRWell cross x (mm)");
            h3c.setTitleY("DC SL" + layer + " cluster average wire");
            H1F h4 = new H1F("hiDCtoUR"+layer, "Distance (cm)", "Counts", 100, 0.0, 10.0);         
            h4.setOptStat(Integer.parseInt("1111")); 
            H1F h5 = new H1F("hiWireL"+layer, "DC SL" + layer + " cluster wire", "Counts", 112, 0, 112);       
            h5.setOptStat(Integer.parseInt("1111")); 
            H1F h5c = new H1F("hiWireCutL"+layer, "DC SL" + layer + " cluster Wire", "Counts", 112, 0, 112);        
            h5c.setOptStat(Integer.parseInt("1111")); 
            h5c.setLineColor(2);
//            dg.addDataSet(h1, il*4 + 0);
            dg.addDataSet(h2,  il*5 + 0);
            dg.addDataSet(h2c, il*5 + 0);
            dg.addDataSet(h3,  il*5 + 1);
            dg.addDataSet(h3c, il*5 + 2);
            dg.addDataSet(h4,  il*5 + 3);
            dg.addDataSet(h5,  il*5 + 4);
            dg.addDataSet(h5c, il*5 + 4);
        }
        dataGroups.put("Matching", dg);

    }
    
    public static void fitGauss(H1F histo) {
        double mean  = histo.getMean();
        double amp   = histo.getBinContent(histo.getMaximumBin());
        double rms   = histo.getRMS();
        double sigma = rms/2;
        double min = mean - 3*rms;
        double max = mean + 3*rms;
        
        F1D f1   = new F1D("f1res","[amp]*gaus(x,[mean],[sigma])", min, max);
        f1.setLineColor(2);
        f1.setLineWidth(2);
        f1.setOptStat("1111");
        f1.setParameter(0, amp);
        f1.setParameter(1, mean);
        f1.setParameter(2, sigma);
            
        if(amp>5) {
            f1.setParLimits(0, amp*0.2,   amp*1.2);
            f1.setParLimits(1, mean*0.5,  mean*1.5);
            f1.setParLimits(2, sigma*0.2, sigma*2);
//            System.out.print("1st...");
            DataFitter.fit(f1, histo, "Q");
            mean  = f1.getParameter(1);
            sigma = f1.getParameter(2);
            f1.setParLimits(0, 0, 2*amp);
            f1.setParLimits(1, mean-sigma, mean+sigma);
            f1.setParLimits(2, 0, sigma*2);
            f1.setRange(mean-2.0*sigma,mean+2.0*sigma);
            DataFitter.fit(f1, histo, "Q");
        }
    }    
    
    public void fillHitHisto(List<Hit> hits) {
        for(Hit hit : hits) {
            if(hit.sector()==0) continue;
            dataGroups.get("Hits").getH1F("hiEnergyL"+hit.layer()).fill(hit.energy());
            dataGroups.get("Hits").getH1F("hiTimeL"+hit.layer()).fill(hit.time());
            dataGroups.get("Hits").getH2F("hiEnergyStripL"+hit.layer()).fill(hit.energy(), hit.strip());
            dataGroups.get("Hits").getH2F("hiTimeStripL"+hit.layer()).fill(hit.time(), hit.strip());
            dataGroups.get("Hits").getH2F("hiTimeEnergyL"+hit.layer()).fill(hit.time(), hit.energy());
        }
    }
    
    public void fillClusterHisto(Event event) {
        for(Cluster cluster : event.getUrwellClusters()) {
            if(cluster.sector()==0) continue;
            dataGroups.get("Clusters").getH1F("hiEnergyL"+cluster.layer()).fill(cluster.energy());
            dataGroups.get("Clusters").getH1F("hiTimeL"+cluster.layer()).fill(cluster.time());
            dataGroups.get("Clusters").getH2F("hiEnergyStripL"+cluster.layer()).fill(cluster.energy(), cluster.strip());
            dataGroups.get("Clusters").getH2F("hiTimeStripL"+cluster.layer()).fill(cluster.time(), cluster.strip());
            if(event.getMc()!=null) {
                Point3D traj = event.swimMcToPlane(swim, cluster.sector(), cluster.localZ());
                Line3D distance = cluster.localLine().distance(traj);
                double delta = distance.length()*Math.signum(distance.direction().x());
                dataGroups.get("Clusters").getH1F("hiDistanceL"+cluster.layer()).fill(delta*10);
            }             
        }
    }
    
    public void fillCrossHisto(Event event) {
        for(Cross cross : event.getUrwellCrosses()) {
            if(cross.sector()==0) continue;
//                    if(!cross.isGood()) continue;
            dataGroups.get("Crosses").getH1F("hiEnergy").fill(cross.energy());
            dataGroups.get("Crosses").getH1F("hiTime").fill(cross.time());
            if(cross.isGood()) {
                dataGroups.get("Crosses").getH1F("hiEnergyGood").fill(cross.energy());
                dataGroups.get("Crosses").getH1F("hiTimeGood").fill(cross.time());                        
                if(event.getMc()!=null) {
                    Point3D traj = event.swimMcToPlane(swim, cross.sector(), cross.local().z());
                    Vector3D delta = cross.local().vectorTo(traj);
                    dataGroups.get("Crosses").getH1F("hiDeltaX").fill(delta.x()*10);
                    dataGroups.get("Crosses").getH1F("hiDeltaY").fill(delta.y()*10);
                }
            }
            dataGroups.get("Crosses").getH2F("hiEnergyX").fill(cross.energy(), cross.local().x());
            dataGroups.get("Crosses").getH2F("hiTimeX").fill(cross.time(), cross.local().x());
            dataGroups.get("Crosses").getH2F("hiEnergy2D").fill(cross.getCluster1().energy(), cross.getCluster2().energy());
            dataGroups.get("Crosses").getH2F("hiTime2D").fill(cross.getCluster1().time(), cross.getCluster2().time());
        }
    }

    public static void main (String[] args)  {

        OptionParser parser = new OptionParser("urwell");
        parser.setRequiresInputList(false);
        parser.addOption("-o",  "", "output event file name");
        parser.addOption("-m", "0", "match based on hits (0) or clusters (1)");
        parser.addOption("-d", "0", "label unmatched hits (0) or drop them (1)");
        parser.addOption("-n","-1", "maximum number of events to process");
        parser.addOption("-w", "1", "open graphical window (1) or run in batch mode (0)");
        parser.parse(args);
        
        
        String output = null;
        if(!parser.getOption("-o").stringValue().isBlank()) 
            output = parser.getOption("-o").stringValue();
        boolean hitmatch  = parser.getOption("-m").intValue()==0;
        boolean drop      = parser.getOption("-d").intValue()==1;
        int     maxEvents = parser.getOption("-n").intValue();
        boolean window    = parser.getOption("-w").intValue()==1;
        
        if(!window) System.setProperty("java.awt.headless", "true");
        DefaultLogger.debug();
        
        URWell analysis = new URWell();        

        HipoDataSync    writer = new HipoDataSync();
        if(output!=null) writer.open(output);
        
        List<String> inputFiles = parser.getInputList();
        
        ProgressPrintout progress = new ProgressPrintout();

        int counter=-1;

        for(String input : inputFiles) {
            HipoDataSource  reader = new HipoDataSource();
            reader.open(input);
            while(reader.hasEvent()) {
                
                counter++;
                
                DataEvent ev = reader.getNextEvent();

                Event event = new Event(ev, URWell.dcWires);

                event.match(hitmatch);
                
                if(!event.getUrwellHits().isEmpty()) {
                    analysis.fillHitHisto(event.getUrwellHits());
                }
                if(!event.getUrwellClusters().isEmpty()) {
                    analysis.fillClusterHisto(event);
                }
                if(!event.getUrwellCrosses().isEmpty() && !event.getUrwellClusters().isEmpty()) {
                    analysis.fillCrossHisto(event);
                    for(Cluster cluster : event.getUrwellClusters()) {
                        if(cluster.sector()==0) continue;
                        analysis.getGroup("Matching").getH1F("hiTimeL"+cluster.layer()).fill(cluster.time());
                        if(cluster.getCrossIndex()>=0) {
                            Cross cross = event.getUrwellCrosses().get(cluster.getCrossIndex());
                            if(cross.isGood() && cross.isInTime()) {
                                analysis.getGroup("Matching").getH1F("hiTimeCutL"+cluster.layer()).fill(cluster.time());
                            }
                        }
                    }
                }
                if(!hitmatch) {
                    for(DCCluster wire : event.getDcClusters()) {
                        if(wire.sector()!=0 && wire.superlayer()<3) {
                            analysis.getGroup("Matching").getH1F("hiWireL" + wire.superlayer()).fill(wire.avgWire());
                            for(Cross cross : event.getUrwellCrosses()) {
                                if(cross.sector()==wire.sector()) {
                                    analysis.getGroup("Matching").getH2F("hiDCL" + wire.superlayer()).fill(cross.local().x(), wire.avgWire());
                                    analysis.getGroup("Matching").getH1F("hiDCtoUR" + wire.superlayer()).fill(cross.distance(wire.line()));
                                    if(cross.isMatched(wire)) 
                                        analysis.getGroup("Matching").getH2F("hiDCCutL" + wire.superlayer()).fill(cross.local().x(), wire.avgWire());
                                }
                            }
                            if(wire.isMatched()) 
                                analysis.getGroup("Matching").getH1F("hiWireCutL" + wire.superlayer()).fill(wire.avgWire());
                        }
                    }
                }
                else {
                    for(DCHit wire : event.getDcHits()) {
                        if(wire.sector()!=0 && wire.layer()<13) {
                            analysis.getGroup("Matching").getH1F("hiWireL" + wire.superlayer()).fill(wire.component());
                            for(Cross cross : event.getUrwellCrosses()) {
                                if(cross.sector()==wire.sector()) {
                                    analysis.getGroup("Matching").getH2F("hiDCL" + wire.superlayer()).fill(cross.local().x(), wire.component());
                                    analysis.getGroup("Matching").getH1F("hiDCtoUR" + wire.superlayer()).fill(cross.distance(wire.line()));
                                    if(cross.isMatched(wire))
                                        analysis.getGroup("Matching").getH2F("hiDCCutL" + wire.superlayer()).fill(cross.local().x(), wire.component());
                                }
                            }
                            if(wire.isMatched()) 
                                analysis.getGroup("Matching").getH1F("hiWireCutL" + wire.superlayer()).fill(wire.component());
                        }
                    }
                }
                if(output!=null) writer.writeEvent(event.reWrite(ev, drop));
                                    
                progress.updateStatus();
                
                if(maxEvents>0){
                    if(counter>=maxEvents) break;
                }

            }
            progress.showStatus();
            reader.close();
        }
        
        if(output!=null) writer.close();
//        for(int i=0; i<NLAYER; i++) {
//            URWell.fitGauss(analysis.getGroup("Matching").getH1F("hiTimeL"+(i+1)));
//            URWell.fitGauss(analysis.getGroup("Matching").getH1F("hiSpace"+axes[i]));
//        }
        if(window) {
            JFrame frame = new JFrame("URWell Reconstruction");
            frame.setSize(1500,800);
            frame.add(analysis.getCanvas());
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);     
        }
    }
}    

