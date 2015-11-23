package cnr.mybeacon;

import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class MainActivity extends ActionBarActivity implements BeaconConsumer {
    protected static final String TAG = "RangingActivity";
    private BeaconManager beaconManager;
    private TextView statusTxt1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusTxt1 = (TextView)findViewById(R.id.statusTxt1);
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.setForegroundScanPeriod(2000);
        beaconManager.bind(this);
    }

    Comparator<Beacon> beaconComparator = new Comparator<Beacon>() {
        @Override
        public int compare(Beacon lhs, Beacon rhs) {
            return lhs.getDistance() > rhs.getDistance() ? 1 : -1;
        }
    };

    public static final int[] ids = new int[]{ 9677, 11819, 27520, 57746 };
    public static final double[][] positions = new double[][] {{0.0, 3.0}, {2.4, 0.0}, {0.0, 0.0}, {2.4, 3.0}};

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(final Collection<Beacon> beacons, Region region) {

                final ArrayList<Beacon> _beacons = new ArrayList<Beacon>();
                Iterator<Beacon> _bs = beacons.iterator();
                double[] distances = new double[] {0, 0, 0, 0};
                final StringBuilder sb = new StringBuilder();
                while(_bs.hasNext()){
                    Beacon _b = _bs.next();
                    int idx = Arrays.binarySearch(ids, _b.getId3().toInt());
                    if(idx >= 0){
                        _beacons.add(_b);
                        distances[idx] = _b.getDistance();
                    }
                }

                for(int i = 0; i < ids.length; i++){
                    if(i != 0)
                        sb.append(", ");
                    sb.append(String.format("%.1f", distances[i]));
                }
                Collections.sort(_beacons, beaconComparator);


                NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
                LeastSquaresOptimizer.Optimum optimum = solver.solve();

                final double[] centroid = optimum.getPoint().toArray();


                if (_beacons.size() > 0) {
                    //find the closest beacon
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Beacon nearest = _beacons.get(0);
                            statusTxt1.setText(String.format("%d || %5d = %.1f m", _beacons.size(), nearest.getId3().toInt(), nearest.getDistance()));
                            ((TextView)findViewById(R.id.resultTv)).setText(String.format("Ans: %.1f, %.1f", centroid[0], centroid[1]));
                            ((TextView)findViewById(R.id.matrixTv)).setText((sb.toString()));
                        }
                    });
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
            Log.d(TAG, "ranging has been started");
        } catch (RemoteException e) {
            Log.e(TAG, "fail to start ranging", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
