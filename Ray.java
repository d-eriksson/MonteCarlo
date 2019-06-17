import javax.vecmath.*;
import java.lang.Math;
import java.util.*;

public class Ray{
    private static final int CHILDREN = 32;
    private static final int CHILDREN_SPECULAR = 32;
    Vector3d start;
    Vector3d end;
    public double RayLength;
    Vector3d direction = new Vector3d(0.0, 0.0, 0.0);
    Vector3d P_hit;
    Vector3d P_Normal;
    Boolean MotherNode;
    Ray(Vector3d s, Vector3d e, Boolean MN){
        start = s;
        end = e;
        this.MotherNode = MN;
        direction.sub(e, s);
        RayLength = Utilities.vecNorm(direction);
        direction.normalize();
    }
    ColorDbl CastRay(Scene S, int Depth){
        Object3D HitObject = S.triangleIntersect(this);
        if(HitObject.mat.Emissive){
            // Ska returnera med hänsyn av distans och ljusets styrka.
            ColorDbl c = new ColorDbl(1.0,1.0,1.0);
            //c.divide(Depth +1 );
            return c;
        }

        ColorDbl DirectLightcontrib = new ColorDbl(0.0,0.0,0.0);
        ColorDbl IndirectLightcontrib = new ColorDbl(0.0,0.0,0.0);
        Double TotalBrightness = 0.0;
        Double Brightness = 0.0;
        Vector3d P_hit_corr = Utilities.vecAdd(P_hit,Utilities.vecScale(P_Normal, 0.01));
        for(Object3D l : S.lightList){
            Vector<Vector3d> SampList = l.getSampleLight();
            Brightness = 0.0;
            for(Vector3d pos : SampList){ // Don't do this, Integrera över ljuskällan på hemispheren. 
                Ray ShadowRay = new Ray(P_hit_corr, pos,false);
                if(!S.ObjectHit(ShadowRay)){  
                    Brightness +=  l.mat.Brightness * Math.max(0.0, Utilities.vecDot(ShadowRay.direction, this.P_Normal))/(ShadowRay.RayLength);
                }
            }
            Brightness /= SampList.size();
            TotalBrightness += Brightness;
            
        }
        TotalBrightness /= S.lightList.size();
        
        DirectLightcontrib.setColor(HitObject.mat.color); 
        DirectLightcontrib.multiply(TotalBrightness);
        if(Depth >= S.MAX_DEPTH) {
            //DirectLightcontrib.divide(Depth+1);
            return DirectLightcontrib;
        }
        

        // Calculate World2Local and Local2World Matrices
        Vector3d Z = P_Normal;
        Vector3d I_o = Utilities.vecSub(direction, Utilities.vecScale(Z, Utilities.vecDot(direction, Z)));
        Vector3d X = Utilities.vecScale(I_o, 1.0/Utilities.vecNorm(I_o));
        Vector3d Y = Utilities.vecCross(Utilities.vecScale(X,-1.0), Z);

        //Create coordinate system transformation matrices
        Matrix4d rotation_mat = new Matrix4d(
        X.x, X.y, X.z, 0.0,
        Y.x, Y.y, Y.z, 0.0,
        Z.x, Z.y, Z.z, 0.0,
        0.0, 0.0, 0.0, 1.0);
        Matrix4d translation_mat = new Matrix4d(
        1.0, 0.0, 0.0, -P_hit.x,
        0.0, 1.0, 0.0, -P_hit.y,
        0.0, 0.0, 1.0, -P_hit.z,
        0.0, 0.0, 0.0, 1.0);
        
        Matrix4d world2local = new Matrix4d();
        world2local.mul(rotation_mat, translation_mat);
        Matrix4d local2world = Utilities.invertMat(world2local);

        //Incoming ray in local coords
        Vector3d direction_local = Utilities.mulMatVec(world2local, direction);
        double Azimuth = 0; //phi
        double Altitude = 0; //theta
        double x,y,z = 0;
        Vector3d LocalEndPoint, EndPoint;
        Vector3d localnormal = new Vector3d(0.0, 0.0, 1.0);
        int N_CHILDREN = 0;

        if(HitObject.mat.isSpecular){
            LocalEndPoint = Utilities.vecSub( Utilities.vecScale(localnormal, Utilities.vecDot(localnormal, direction_local)), direction_local);
            Random random = new Random();
            for(int i = 0; i < CHILDREN_SPECULAR; ++i){
                Azimuth = Math.PI - (random.nextDouble()*2.0*Math.PI)/1.0; /* Kolla över denna sektion och utvärdera matematiken för Azi och Alt */
                Altitude = 0.25*Math.PI- (random.nextDouble()*0.5*Math.PI)/1.0;
                LocalEndPoint.x += Math.sin(Altitude)*Math.sin(Azimuth);
                LocalEndPoint.y += Math.sin(Altitude)*Math.cos(Azimuth);
                LocalEndPoint.z += Math.cos(Altitude); // up
  
                EndPoint = Utilities.mulMatVec(local2world, LocalEndPoint);
                IndirectLightcontrib.sumColor(new Ray(P_hit_corr, EndPoint, false).CastRay(S,Depth+1));
                N_CHILDREN++;
            }
          }
        else{
            LocalEndPoint = new Vector3d();
            Random random = new Random();
            for(int i = 0; i < CHILDREN; ++i){
                if(!Utilities.RussianBullet(S.DEPTH_DECAY + (1.0-S.DEPTH_DECAY) * ((double)Depth / (double)S.MAX_DEPTH))){
                    Azimuth = random.nextDouble()*2*Math.PI;
                    Altitude = random.nextDouble()*0.5*Math.PI;
                    LocalEndPoint.x = Math.sin(Altitude)*Math.sin(Azimuth);
                    LocalEndPoint.y = Math.sin(Altitude)*Math.cos(Azimuth);
                    LocalEndPoint.z = Math.cos(Altitude); // up
    
                    EndPoint = Utilities.mulMatVec(local2world, LocalEndPoint);
                    Ray Child = new Ray(P_hit_corr, EndPoint, false);
                    ColorDbl c = Child.CastRay(S, Depth+1);
                    IndirectLightcontrib.sumColor(c);
                    N_CHILDREN++;
                }
            }
        }
        if(N_CHILDREN != 0){
            IndirectLightcontrib.divide(N_CHILDREN);
        }
        IndirectLightcontrib.multiply(HitObject.mat.color);
        DirectLightcontrib.sumColor(IndirectLightcontrib);
        //DirectLightcontrib.divide(Depth+1);
        return DirectLightcontrib;
    }
    
    void calculatePhit(double t){
        P_hit = new Vector3d();
        P_hit = Utilities.vecSub(end,start);
        P_hit.scale(t);
        P_hit = Utilities.vecAdd(P_hit, start);
    }

    void print(){
        System.out.println("Start vector: ( " +start.x+", "+start.y+", "+start.z+" ) \nEnd vector: ( "+end.x+", "+end.y+", "+end.z+" )");
    }
    public static void main(String[] args) {
        Vector3d v1 = new Vector3d(1.0,2.0,3.0);
        Vector3d v2 = new Vector3d(2.0,1.0,2.0);
        Ray r1 = new Ray(v1,v2,true);
        r1.print();
    }
}
