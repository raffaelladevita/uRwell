package org.jlab.clas12.urwell;

import java.util.ArrayList;
import java.util.List;
import org.jlab.clas.physics.Particle;
import org.jlab.clas.swimtools.Swim;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.base.DetectorType;
import org.jlab.geom.prim.Line3D;
import org.jlab.geom.prim.Plane3D;
import org.jlab.geom.prim.Point3D;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;

/**
 *
 * @author devita
 */
public class Event {
   
    private final List<Hit>       urHits     = new ArrayList<>();
    private final List<Cluster>   urClusters = new ArrayList<>();
    private final List<Cross>     urCrosses  = new ArrayList<>();
    private final List<DCCluster> dcClusters = new ArrayList<>();
    private final List<DCHit>     dcHits     = new ArrayList<>();
    
    private Particle mc = null;
    
    private final double deltaE = 200;
    private final double deltaT = 50;
    private final double meanT = 180;
    private final double maxDist = 1;
    private final double targetz = -3;
    
    public Event(DataEvent event, Line3D[][] dcWires) {
        
//            double xtrue = 0;
//            double ytrue = 0;
//            double ztrue = 0;
//            Point3D mc = null;
//            if(ev.hasBank("MC::True")) {
//                DataBank bankMC = ev.getBank("MC::True");
//                for(int i=0; i<bankMC.rows(); i++) {
//                    int detector = bankMC.getByte("detector",i);  
//                    if(detector==DetectorType.URWELL.getDetectorId()) {
//                        xtrue = bankMC.getFloat("avgX",i);
//                        ytrue = bankMC.getFloat("avgY",i);
//                        ztrue = bankMC.getFloat("avgZ",i);
//                        if(mc==null) mc = new Point3D(xtrue, ytrue, ztrue);
//                    }
//                }
//            }
//            if(mc!=null) mc.rotateY(-Math.toRadians(25.0));
//            System.out.println(mc);
        if(event.hasBank("MC::Particle"))
            this.readMCParticle(event.getBank("MC::Particle"));
        if(event.hasBank("URWELL::hits"))
            this.readHits(event.getBank("URWELL::hits"));
        if(event.hasBank("URWELL::clusters"))
            this.readClusters(event.getBank("URWELL::clusters"));
        if(event.hasBank("URWELL::crosses"))
            this.readCrosses(event.getBank("URWELL::crosses"));
        if(event.hasBank("DC::tdc"))
            this.readDcTDCs(event.getBank("DC::tdc"), dcWires);
        if(event.hasBank("HitBasedTrkg::Clusters"))
            this.readDcClusters(event.getBank("HitBasedTrkg::Clusters"), dcWires);
    }

    public Particle getMc() {
        return mc;
    }

    public Point3D swimMcToPlane(Swim swim, int sector, double z) {
        Point3D V    = new Point3D(mc.vx(),mc.vy(),mc.vz());
        Point3D P    = new Point3D(mc.px(),mc.py(),mc.pz());
        URWell.TOLOCAL[sector-1].apply(V);
        URWell.TOLOCAL[sector-1].apply(P);
        swim.SetSwimParameters(V.x(), V.y(), V.z(), P.x(), P.y(), P.z(), mc.charge());
        double[] swimResults = swim.SwimToPlaneTiltSecSys(sector,z);
        Point3D swimV = new Point3D(swimResults[0],swimResults[1],swimResults[2]);
//        Point3D swimP = new Point3D(swimResults[3],swimResults[4],swimResults[5]);
        return swimV;
    }
    
    public List<Hit> getUrwellHits() {
        return urHits;
    }

    public List<Cluster> getUrwellClusters() {
        return urClusters;
    }

    public List<Cross> getUrwellCrosses() {
        return urCrosses;
    }

    public List<DCCluster> getDcClusters() {
        return dcClusters;
    }

    public List<DCHit> getDcHits() {
        return dcHits;
    }
    
    public List<DCHit> getSelectedDcHits() {
        List<DCHit> hits = new ArrayList<>();
        for(DCHit h : this.dcHits) 
            if(h.isMatched()) hits.add(h);
        return hits;
    }
    
    private void readMCParticle(DataBank bank) {
        if(mc==null) {
            mc = new Particle(bank.getInt("pid", 0),
                              bank.getFloat("px", 0),
                              bank.getFloat("py", 0),
                              bank.getFloat("pz", 0),
                              bank.getFloat("vx", 0),
                              bank.getFloat("vy", 0),
                              bank.getFloat("vz", 0));
        }
        
    }
    
    public final void readHits(DataBank bank) {

        for(int i=0; i<bank.rows(); i++) {
            int    sector = bank.getByte("sector", i);
            int    layer  = bank.getByte("layer", i);
            int    strip  = bank.getShort("strip", i);
            double energy = bank.getFloat("energy", i);
            double time   = bank.getFloat("time", i);
            Hit hit = new Hit(sector, layer, strip, energy, time);
            urHits.add(hit);
        }
    }
    
    public final void readClusters(DataBank bank) {

        for(int i=0; i<bank.rows(); i++) {
            int    sector = bank.getByte("sector", i);
            int    layer  = bank.getByte("layer", i);
            int    strip  = bank.getShort("strip", i);
            int    size   = bank.getShort("size", i);
            double energy = bank.getFloat("energy", i);
            double time   = bank.getFloat("time", i);
            Cluster cluster = new Cluster(sector, layer, strip, size, energy, time);
            cluster.line(bank.getFloat("xo", i),
                         bank.getFloat("yo", i),
                         bank.getFloat("zo", i),
                         bank.getFloat("xe", i),
                         bank.getFloat("ye", i),
                         bank.getFloat("ze", i));
            urClusters.add(cluster);
        }
    }
    
    public final void readCrosses(DataBank bank) {

        for(int i=0; i<bank.rows(); i++) {
            int    sector = bank.getByte("sector", i);
            double x      = bank.getFloat("x", i);
            double y      = bank.getFloat("y", i);
            double z      = bank.getFloat("z", i);                        
            double energy = bank.getFloat("energy", i);
            double time   = bank.getFloat("time", i);
            int  cluster1 = bank.getShort("cluster1", i);
            int  cluster2 = bank.getShort("cluster2", i); 
            Cross cross = new Cross(sector, x, y, z, energy, time);
            cross.setClusterIndex1(cluster1);
            cross.setClusterIndex2(cluster2);
            if(cluster1<=urClusters.size()) urClusters.get(cluster1-1).setCrossIndex(i);
            if(cluster2<=urClusters.size()) urClusters.get(cluster2-1).setCrossIndex(i);
            urCrosses.add(cross);
        }
    }    
    
        
    public final void readDcClusters(DataBank bank, Line3D[][] wires) {
        
        for(int j=0; j<bank.rows(); j++) {
            int    id         = bank.getShort("id", j);
            int    status     = bank.getShort("status", j);
            int    sector     = bank.getByte("sector", j);
            int    superlayer = bank.getByte("superlayer", j);
            double avgWire    = bank.getFloat("avgWire", j);
            double fitInterc  = bank.getFloat("fitInterc", j);
            double fitSlope   = bank.getFloat("fitSlope", j);
            double fitIntercErr  = bank.getFloat("fitIntercErr", j);
            double fitSlopeErr   = bank.getFloat("fitSlopeErr", j);
            double fitChisqProb   = bank.getFloat("fitChisqProb", j);
            DCCluster wire = new DCCluster(id, sector, superlayer, avgWire, status);
            for(int i=0; i<12; i++) {
                int hitId = bank.getShort("Hit"+(i+1)+"_ID", j);
                if(hitId>0 && dcHits!=null && dcHits.size()>=hitId)
                    wire.add(dcHits.get(hitId-1));
            }
            wire.setFitParameters(fitInterc, fitSlope, fitIntercErr, fitSlopeErr, fitChisqProb);
            wire.setLine(wires[superlayer-1][(int) avgWire -1]);
            dcClusters.add(wire);
        }
    }    
    
    public final void readDcTDCs(DataBank bank, Line3D[][] wires) {
        
        for(int j=0; j<bank.rows(); j++) {
            int id        = j+1;
            int sector    = bank.getByte("sector", j);
            int layer     = bank.getByte("layer", j);
            int component = bank.getShort("component", j);
            int order     = bank.getByte("order", j);
            int tdc       = bank.getInt("TDC", j);

            DCHit wire = new DCHit(id, sector, layer, component, order, tdc);
            wire.setLine(wires[wire.layer()-1][component -1]);
            dcHits.add(wire);
        }
    }    
    
    public DataEvent reWriteDcClusters(DataEvent event, List<DCCluster> wires) {

        String name = "HitBasedTrkg::Clusters";

        event.removeBank(name);
        DataBank bank = event.createBank(name, wires.size());

        for(int i=0; i<wires.size(); i++) {

            bank.setShort("id", i, (short) wires.get(i).id());
            bank.setShort("status", i, (short) wires.get(i).status());
            bank.setByte("superlayer", i, (byte) wires.get(i).superlayer());
            bank.setByte("sector", i, (byte) wires.get(i).sector());
            bank.setFloat("avgWire", i, (float) wires.get(i).avgWire());
            bank.setByte("size", i, (byte) wires.get(i).size());
            bank.setFloat("fitInterc",    i, (float) wires.get(i).fitPars[0]);
            bank.setFloat("fitSlope",     i, (float) wires.get(i).fitPars[1]);
            bank.setFloat("fitIntercErr", i, (float) wires.get(i).fitPars[2]);
            bank.setFloat("fitSlopeErr",  i, (float) wires.get(i).fitPars[3]);
            bank.setFloat("fitChisqProb", i, (float) wires.get(i).fitPars[4]);
            for (int j = 0; j<12; j++) {
                int id = -1;
                if(j<wires.get(i).size()) id = wires.get(i).get(j).id();
                bank.setShort("Hit"+(j+1)+"_ID", i, (short) id);
            }
        }
        event.appendBank(bank);
        return event;
    }
    
    public DataEvent reWrite(DataEvent event, boolean drop) {

        String name = "DC::tdc";
        event.removeBank(name);
        
        List<DCHit> wires = this.getDcHits();
        if(drop) wires = this.getSelectedDcHits();
        
        DataBank bank = event.createBank(name, wires.size());
        for(int i=0; i<wires.size(); i++) {

            bank.setByte("sector", i, (byte) wires.get(i).sector());
            bank.setByte("layer", i, (byte) wires.get(i).layer());
            bank.setShort("component", i, (short) wires.get(i).component());
            if(wires.get(i).isMatched()) 
                bank.setByte("order", i, (byte) wires.get(i).order());
            else 
                bank.setByte("order", i, (byte) (wires.get(i).order()+10)); 
            bank.setInt("TDC", i, wires.get(i).tdc());
        }
        event.appendBank(bank);
        return event;
    }

    private void matchToClusters() {
        for(DCCluster wire : this.dcClusters) {
            if(wire.sector()==1 && wire.superlayer()<3) {
                for(Cross cross : this.urCrosses) {
                    if(cross.isMatched(wire))
                        wire.setMatchStatus(true);
                }
            }
        }
    }

    private void matchToHits() {
        for(DCHit wire : this.dcHits) {
            if(wire.sector()!=0 && wire.superlayer()<3) {
                for(Cross cross : this.urCrosses) {
                    if(cross.isMatched(wire))
                        wire.setMatchStatus(true);
                }
            }
        }
    }
    
    public void match(boolean mode) {
        if(mode)
            this.matchToHits();
        else
            this.matchToClusters();
    }
    
    public class Hit {
        
        private DetectorDescriptor  desc = new DetectorDescriptor(DetectorType.URWELL);    
        private double    energy = 0;   
        private double      time = 0;
    
        public Hit(int sector, int layer, int component, double energy, double time){
            this.desc.setSectorLayerComponent(sector, layer, component);
            this.energy = energy;
            this.time   = time;
        }

        public int sector() {
            return this.desc.getSector();
        }

        public int layer() {
            return this.desc.getLayer();
        }

        public int strip() {
            return this.desc.getComponent();
        }
        
        public double energy() {
            return energy;
        }

        public double time() {
            return time;
        }

    }

    public class Cluster {
        
        private DetectorDescriptor  desc = new DetectorDescriptor(DetectorType.URWELL);  
        private int         size = 0;
        private double    energy = 0;   
        private double      time = 0;
        private Line3D      line = null;
        private int   crossIndex = -1;
        
        public Cluster(int sector, int layer, int component, int size, double energy, double time){
            this.desc.setSectorLayerComponent(sector, layer, component);
            this.size   = size;
            this.energy = energy;
            this.time   = time;
        }

        public int sector() {
            return this.desc.getSector();
        }

        public int layer() {
            return this.desc.getLayer();
        }

        public int strip() {
            return this.desc.getComponent();
        }

        public int size() {
            return size;
        }
        
        public double energy() {
            return energy;
        }

        public double time() {
            return time;
        }

        public Line3D line() {
            return line;
        }

        public void line(double xo, double yo, double zo, double xe, double ye, double ze) {
            this.line = new Line3D(xo, yo, zo, xe, ye, ze);
        }

        public Line3D localLine() {
            Line3D local = new Line3D(line);
            URWell.TOLOCAL[this.sector()-1].apply(local);
            return local;
        }
        
        public double localZ() {
            return this.localLine().origin().z();
        }
        
        public int getCrossIndex() {
            return crossIndex;
        }

        public void setCrossIndex(int crossIndex) {
            this.crossIndex = crossIndex;
        }

    }

    
    public class Cross {
        
        private DetectorDescriptor  desc = new DetectorDescriptor(DetectorType.URWELL);    
        private Point3D global;   
        private Point3D local;   
        private double    energy = 0;   
        private double      time = 0;
        private int     cluster1 = -1;
        private int     cluster2 = -1;
    
        public Cross(int sector, double x, double y, double z, double energy, double time){
            this.desc.setSectorLayerComponent(sector, 0, 0);
            this.global = new Point3D(x, y, z);
            this.local  = new Point3D(x, y, z);
            local.rotateZ(Math.toRadians(-60*(sector-1)));
            local.rotateY(Math.toRadians(-25));
            this.energy = energy;
            this.time   = time;
        }

        public int sector() {
            return this.desc.getSector();
        }

        public Point3D position() {
            return this.global;
        }
        
        public Point3D local() {
            return this.local;
        }
        
        public double energy() {
            return energy;
        }

        public double time() {
            return time;
        }

        public void setClusterIndex1(int cluster) {
            this.cluster1 = cluster;
        }

        public void setClusterIndex2(int cluster) {
            this.cluster2 = cluster;
        }
        
        public Cluster getCluster1() {
            if(cluster1>0 && cluster1<=urClusters.size())
                return urClusters.get(cluster1-1);
            else 
                return null;
        }
        
        public Cluster getCluster2() {
            if(cluster2>0 && cluster2<=urClusters.size())
                return urClusters.get(cluster2-1);
            else 
                return null;
        }

        public boolean isGood() {
            return Math.abs(this.getCluster1().energy()-this.getCluster2().energy())<deltaE &&
                   Math.abs(this.getCluster1().time()-this.getCluster2().time())<deltaT; 
        }

        public boolean isInTime() {
            return Math.abs(this.time()-meanT)<1.2*deltaT; 
        }
        
        public boolean isMatched(DCCluster c) {
            if(this.sector()!=c.sector()) return false;
            return this.isMatched(c.line());
        }
         
        public boolean isMatched(DCHit c) {
            if(this.sector()!=c.sector()) return false;
            return this.isMatched(c.line());
        }
                            
        private boolean isMatched(Line3D wire) {
            return this.distance(wire)<maxDist && this.isGood() && this.isInTime();
        }            

        public double distance(Line3D wire) {
            Line3D crossLine = new Line3D(this.position(), this.position().vectorFrom(0, 0, targetz).asUnit());
            URWell.TOLOCAL[this.sector()-1].apply(crossLine);
            Plane3D dcLayer = new Plane3D(0, 0, wire.origin().z(), 0, 0, 1);
            Point3D crossProjection = new Point3D();
            int nint = dcLayer.intersection(crossLine, crossProjection);
            if(nint>0) {
                return wire.distance(crossProjection).length();
            }
            else {
                return Double.MAX_VALUE;
            }
        }  
    }

       
    public class DCCluster extends ArrayList<DCHit> {
        
        private int id = 0;
        private int status = 0;
        private int sector = 0;
        private int superlayer = 0;
        private double avgWire = 0;
        private double[] fitPars = new double[5];
        private Line3D line;
        
        private boolean match=false;
    
        public DCCluster(int id, int sector, int superlayer, double avgWire, int status) {
            this.id = id;
            this.sector = sector;
            this.superlayer = superlayer;
            this.avgWire = avgWire;
            this.status = status;
            if(sector==0 || superlayer>2)
                this.match = true;
        }

        public void setFitParameters(double interc, double slope, double intercErr, double slopeErr, double probability) {
            this.fitPars[0] = interc;
            this.fitPars[1] = slope;
            this.fitPars[2] = intercErr;
            this.fitPars[3] = slopeErr;
            this.fitPars[4] = probability;           
        }

        public void setLine(Line3D line) {
            this.line = line;
        }

        public int id() {
            return id;
        }

        public int status() {
            return status;
        }

        public int sector() {
            return this.sector;
        }

        public int superlayer() {
            return this.superlayer;
        }

        public double avgWire() {
            return this.avgWire;
        }

        public double[] fitPars() {
            return fitPars;
        }

        public Line3D line() {
            return line;
        }

        public boolean isMatched() {
            return match;
        }

        public void setMatchStatus(boolean match) {
            this.match = match;
            for(DCHit h : this)
                h.setMatchStatus(match);
        }

    }

    public class DCHit {
        
        private int id = 0;
        private int sector = 0;
        private int layer = 0;
        private int component = 0;
        private int order = 0;
        private int tdc = 0;
        private Line3D line;
        private boolean match=false;
    
        public DCHit(int id, int sector, int layer, int component, int order, int tdc){
            this.id = id;
            this.sector = sector;
            this.layer = layer;
            this.component = component;
            this.order = order;
            this.tdc = tdc;
            if(sector==0 || layer>12)
                this.match = true;
        }

        public void setLine(Line3D line) {
            this.line = line;
        }

        public int id() {
            return this.id;
        }

        public int sector() {
            return this.sector;
        }

        public int layer() {
            return this.layer;
        }

        public int superlayer() {
            return (int) (this.layer-1)/6+1;
        }

        public int component() {
            return this.component;
        }

        public int order() {
            return order;
        }

        public int tdc() {
            return tdc;
        }

        public Line3D line() {
            return line;
        }

        public boolean isMatched() {
            return match;
        }

        public void setMatchStatus(boolean match) {
            this.match = match;
        }

    }

}
