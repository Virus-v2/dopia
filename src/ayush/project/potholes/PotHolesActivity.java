package ayush.project.potholes;

import android.os.*;
import android.graphics.Color;
import android.hardware.*;
import android.widget.TextView;
import android.location.*;
import android.content.*;
import java.util.*;
import android.util.DisplayMetrics;
import java.io.*;
import java.net.*;
import android.view.*;
import com.google.android.maps.*;
import android.graphics.*;
import android.accounts.*;

public class PotHolesActivity extends MapActivity 
{
	SensorManager sensorManager;
	PotHolesActivity a;
	SensorEventListener sel;
	LocationListener locationListener;
	LocationManager locationManager;
	TextView tv;
	Location location1;
	float[] accelerationValues,orientationValues,gravity;
	boolean flag=true,print=true;
	float threshhold1=5;
	float threshhold2=1;
	LinkedList<Integer> ll;
	MapView mp;
	View zoom;
	MapController mcr;
	int ff=0,vv=0;
	List <GeoPoint> list;
	String account;
	BufferedWriter bw;

	class MapOverlay extends Overlay
    {
        public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) 
        {
        	super.draw(canvas, mapView, shadow);
        	for(GeoPoint p:list)
        	{                   
        		Point screenPts = new Point();
        		mp.getProjection().toPixels(p, screenPts);
 
        		Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.pushpin);            
        		canvas.drawBitmap(bmp, screenPts.x-8, screenPts.y-20, null); 
        	}
            return true;
        }
    }
	class MyLocationOverlay extends Overlay
    {
        public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) 
        {
        	super.draw(canvas, mapView, shadow);
        	if(location1.hasAccuracy())
        	{
        		Point screenPts = new Point();
        		mp.getProjection().toPixels(new GeoPoint((int)(location1.getLatitude()*1E6),(int)(location1.getLongitude()*1E6)), screenPts);
 
        		Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.i_location);    
        		canvas.drawBitmap(bmp, screenPts.x-8, screenPts.y-7, null); 
        	}
            return true;
        }
    }
	double speed=0;
	float direction=0;
	int gra=0;
	public void onCreate(Bundle savedInstanceState) 
    {
    	super.onCreate(savedInstanceState);
    	a=this;
    	Account[] acm=AccountManager.get(this).getAccounts();
    	for(Account a:acm)
    		if(a.name.endsWith("gmail.com"))
    			account=a.name;
    	ll=new LinkedList<Integer>();
    	ll.add(0);
    	ll.add(0);
    	ll.add(0);
    	location1=new Location(LocationManager.GPS_PROVIDER);
    	list=new LinkedList<GeoPoint>();
        tv=new TextView(this);
        mp=new MapView(this,"0mljy41KPpG7RtNfLU2rqSMXUtynQ_JcALrPloA");
        mp.setBuiltInZoomControls(true);
        mp.setClickable(true);
        mp.setEnabled(true);
        //mp.setSatellite(false);
        //mp.setTraffic(false);
        //mp.setStreetView(true);
        mp.displayZoomControls(true);
        mp.getOverlays().add(new MapOverlay());
        mp.getOverlays().add(new MyLocationOverlay());
        mcr=mp.getController();
        setContentView(tv);
        
        accelerationValues=new float[3];
        orientationValues=new float[3];
        gravity=new float[3];
        locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() 
        {
            public void onLocationChanged(Location location) 
            {
            	location1=location;
            	if(print)
            		tv.setText("Latitude: "+location.getLatitude()+"\nLongitude: "+location.getLongitude()+"\nAccuracy: "+location.getAccuracy()+"\nAccleration:\nX:"+accelerationValues[0]+"\nY:"+accelerationValues[1]+"\nZ:"+accelerationValues[2]+"\nGravity:\nX: "+gravity[0]+"\nY: "+gravity[1]+"\nZ: "+gravity[2]);
            	if(location.hasSpeed() && location.hasBearing())
            	{
            		speed=location.getSpeed();
            		direction=location.getBearing();
            		new Thread(){
            			public void run()
            			{
            				File sdcard = Environment.getExternalStorageDirectory();
    				        File dir = new File (sdcard.getAbsolutePath() + "/potholeData/");
    				        dir.mkdirs();
    				        File file = new File(dir, "speed_data.txt");

    				        FileWriter f=null;
    				        try{
    				        	f = new FileWriter(file,true);
    				        	bw=new BufferedWriter(f);
        				        bw.write("latitude="+Double.toString(location1.getLatitude())+"&longitude="+Double.toString(location1.getLongitude())+"&speed="+Double.toString(speed)+"&direction="+Float.toString(direction)+"&accuracy="+(int)location1.getAccuracy()+"&time="+new Date().toString()+"\n");
        						bw.flush();
        						bw.close();
    				        }catch(Exception mm)
    				        {}
            			}
            		}.start();
            	}
            	if(location.hasAccuracy())
            	{
            		flag=true;
            		if(ff==0)
            		{
            			a.runOnUiThread(new Runnable()
       					{
       						public void run()
       						{
       							mcr=mp.getController();
       							mcr.animateTo(new GeoPoint((int)(location1.getLatitude()*1E6),(int)(location1.getLongitude()*1E6)));
       							mp.invalidate();
       						}
       					});
            			//ff=1;
            		}
            		new Thread()
                    {
                    	public void run()
                    	{
               				list=getPotHoleList(location1.getLatitude(),location1.getLongitude());
                    	}
                    }.start();
            	}
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sel=new SensorEventListener()
        {
        	public void onSensorChanged(SensorEvent event) 
            {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                {
                	if(gra<200)
                	{
                		getGravity(event);  //low pass filter
                		gra++;
                	}
                    getAccelerometer(event);
                    
                    float denom=(float)Math.sqrt(gravity[0]*gravity[0]+gravity[1]*gravity[1]+gravity[2]*gravity[2]);
                    float x2=gravity[0]/denom,y2=gravity[1]/denom,z2=gravity[2]/denom;
                    //denom=(float)Math.sqrt(accelerationValues[0]*accelerationValues[0]+accelerationValues[1]*accelerationValues[1]+accelerationValues[2]*accelerationValues[2]);
                    float x1=accelerationValues[0],y1=accelerationValues[1],z1=accelerationValues[2];
                    float dotProduct1=x1*x2+y1*y2+z1*z2;
                    float crossx=y1*z2-z1*y2,crossy=-x1*z2+z1*x2,crossz=x1*y2-y1*x2;
                    float cross2x=y2*crossz-z2*crossy,cross2y=-x2*crossz+z2*crossx,cross2z=x2*crossy-y2*crossx;
                    denom=(float)Math.sqrt(cross2x*cross2x + cross2y*cross2y + cross2z*cross2z);
                    float x3=cross2x/denom,y3=cross2y/denom,z3=cross2z/denom;
                    float dotProduct2=x1*x3+y1*y3+z1*z3;
                    
                    if(flag && print)
                		tv.setText("Latitude: "+location1.getLatitude()+"\nLongitude: "+location1.getLongitude()+"\nAccuracy: "+location1.getAccuracy()+"\nAccleration:\nX:"+accelerationValues[0]+"\nY:"+accelerationValues[1]+"\nZ:"+accelerationValues[2]+"\nGravity:\nX: "+gravity[0]+"\nY: "+gravity[1]+"\nZ: "+gravity[2]+"\nSpeed= "+speed+"\nDirection= "+direction);
                    
                    if(dotProduct2 > threshhold2)
                    {
                    	//brake
                    }
                    
                    if(dotProduct1 > threshhold1)
                    {
                    	if(print)
                    		tv.setBackgroundColor(Color.RED);
                    	if(!(ll.get(2)==1))
                    	{
                    		ll.remove();
                    		ll.add(1);
                    	}
                    	if(ll.get(0)==-1)
                    	{
                    		new Thread()
                    		{
                    			public void run()
                    			{
                    				print=false;
                    				a.runOnUiThread(new Runnable()
                   					{
                   						public void run()
                   						{
                   							tv.setBackgroundColor(Color.YELLOW);
                   							tv.setText("\n\n\n\n\n!!                 POT HOLE AT LAT: "+location1.getLatitude()+" AND LON: "+location1.getLongitude()+"                !!");
                   						}
                   					});
                    				if(location1.hasAccuracy())
                    				{
                    					try
                        				{
                        					URL url=new URL("http://potholelocations.ayushmaanbhav.cloudbees.net/pothole.do?latitude="+Double.toString(location1.getLatitude())+"&longitude="+Double.toString(location1.getLongitude())+"&accuracy="+(int)location1.getAccuracy()+"&account="+account.substring(0,account.indexOf('@')));
                        					final HttpURLConnection hh=(HttpURLConnection)url.openConnection();
                        					hh.setDoInput(true);
                        					hh.setDoOutput(true);
                        					hh.setRequestMethod("GET");
                        					hh.connect();
                        					InputStream is=(InputStream)hh.getContent();
                        					String s="";int c;
                        					while((c=is.read())!=-1)
                        					{
                        						s+=(char)c;
                        					}
                        					final String h=s;
                        					a.runOnUiThread(new Runnable()
                           					{
                           						public void run()
                           						{
                           							try{
                           								tv.setText(h);
                           							}catch(Exception n){}
                           						}
                           					});
                    						
                    				        
                    				        /*File sdcard = Environment.getExternalStorageDirectory();
                    				        File dir = new File (sdcard.getAbsolutePath() + "/potholeData/");
                    				        dir.mkdirs();
                    				        File file = new File(dir, "data.txt");

                    				        FileWriter f=null;
                    				        try{
                    				        	f = new FileWriter(file,true);
                    				        }catch(Exception mm)
                    				        {}
                    				        
                    				        bw=new BufferedWriter(f);
                    				        
                    				        //
                    						bw.write("latitude="+Double.toString(location1.getLatitude())+"&longitude="+Double.toString(location1.getLongitude())+"&accuracy="+(int)location1.getAccuracy()+"&time="+new Date().toString()+"\n");
                    						bw.flush();
                    						bw.close();*/
                        				}
                        				catch(Exception m)
                        				{
                        					final String jh=m.getMessage();
                        					a.runOnUiThread(new Runnable()
                           					{
                           						public void run()
                           						{
                           							tv.setText(jh);
                           						}
                           					});
                        				}
                    				}
                   					try
                   					{
                   						Thread.sleep(5000);
                   					}
                   					catch(Exception b)
                   					{}
                   					print=true;
                    			}
                    		}.start();
                    	}
                    }
                    else if(dotProduct1<-threshhold1)            //in a 45 degree cone around gravity
                    {
                    	if(print)
                    		tv.setBackgroundColor(Color.GREEN);
                    	if(!(ll.get(2)==-1))
                    	{
                    		ll.remove();
                    		ll.add(-1);
                    	}
                    }
                    else
                    {
                    	if(print)
                    		tv.setBackgroundColor(Color.BLACK);
                    	if(!(ll.get(2)==0))
                    	{
                    		ll.remove();
                    		ll.add(0);
                    	}
                    }
                }
            }

        	private void getGravity(SensorEvent event)
        	{
        		final float alpha = (float)0.8;
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
        	}
        	
        	private void getAccelerometer(SensorEvent event) 
            {
                accelerationValues[0] = event.values[0] - gravity[0];
                accelerationValues[1] = event.values[1] - gravity[1];
                accelerationValues[2] = event.values[2] - gravity[2];
            }

            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
    }

	float distanceInMeters = 0;
	GeoPoint g1 = null;
	protected List<GeoPoint> getPotHoleList(double latitude,double longitude)
	{
		final List<GeoPoint> ll=new LinkedList<GeoPoint>();
		try
		{
			DisplayMetrics metrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(metrics);
		    GeoPoint g0 = mp.getProjection().fromPixels(0, metrics.heightPixels);
		    /*int lat = (int) (location1.getLatitude() * 1E6);
		    int lng = (int) (location1.getLongitude() * 1E6);
		    GeoPoint g1 = new GeoPoint(lat, lng);*/
		    float[] results = new float[1];
		    Location.distanceBetween(g0.getLatitudeE6()/1E6, g0.getLongitudeE6()/1E6, latitude, longitude, results);
		    distanceInMeters = results[0];
		    
			URL url=new URL("http://potholelocations.ayushmaanbhav.cloudbees.net/retrieve.do?latitude="+Double.toString(latitude)+"&longitude="+Double.toString(longitude)+"&dis="+Double.toString(distanceInMeters));
			final HttpURLConnection hh=(HttpURLConnection)url.openConnection();
			hh.setDoInput(true);
			hh.setDoOutput(true);
			hh.setRequestMethod("GET");
			hh.connect();
			BufferedReader br=new BufferedReader(new InputStreamReader((InputStream)hh.getContent()));
			String s;//lld="";
			while((s=br.readLine())!=null)
			{
				//lld+=s+"\n";
				String[] str=s.split(" ");
				ll.add(new GeoPoint((int)(Double.parseDouble(str[0])*1E6),(int)(Double.parseDouble(str[1])*1E6)));
			}
			//final String hhh=lld;
			//print=false;
			a.runOnUiThread(new Runnable()
			{
				public void run()
				{
					//tv.setText(hhh);
					mp.postInvalidate();
				}
			});
			/*new Thread(){
				public void run()
				{
					try
					{
						Thread.sleep(5000);
					}
					catch(Exception b)
					{}
					print=true;
				}
			}.start();*/
		}
		catch(final Exception mm)
		{
			/*print=false;
			a.runOnUiThread(new Runnable()
			{
				public void run()
				{
					tv.setText(mm.getMessage());
					mp.postInvalidate();
				}
			});
			new Thread(){
				public void run()
				{
					try
					{
						Thread.sleep(5000);
					}
					catch(Exception b)
					{}
					print=true;
				}
			}.start();*/
		}
		return ll;
	}
	
    protected void onStart()
    {
    	super.onStart();
    	a.runOnUiThread(new Runnable()
        {
        	public void run()
        	{
        		tv.setText("!! Hello !!\n!! Getting Location and Accleration !!");
        	}
        });
        
        try
        {
          	final Location l=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
          	a.runOnUiThread(new Runnable()
            {
            	public void run()
            	{
            		tv.setText("Latitude: "+l.getLatitude()+"\nLongitude: "+l.getLongitude()+"\nAccuracy: "+l.getAccuracy());
            	}
            });
          	flag=true;
        }
        catch(Exception e)
        {
        	a.runOnUiThread(new Runnable()
            {
            	public void run()
            	{
            		tv.setText("!! Cannot get last known location !!\n!! Waiting for Location !!");
            	}
            });
        }
    }
    
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        menu.add("Map Window");
        menu.add("Colsole Window");
        menu.add("My Location");
        menu.add("Refresh Map");
        
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        if(((String)item.getTitle()).equals("Map Window"))
        {
           	a.runOnUiThread(new Runnable()
			{
				public void run()
				{
					a.setContentView(mp);
					vv=0;
				}
			});
        }
        else if(((String)item.getTitle()).equals("Colsole Window"))
        {
        	a.runOnUiThread(new Runnable()
			{
				public void run()
				{
					a.setContentView(tv);
					vv=1;
				}
			});
        }
        else if(((String)item.getTitle()).equals("Refresh Map"))
        {
        	if(vv==0)
        	{
        		new Thread()
                {
                	public void run()
                	{
                		GeoPoint p=mp.getMapCenter();
           				list=getPotHoleList(((double)p.getLatitudeE6())/(double)1E6,((double)p.getLongitudeE6())/(double)1E6);
                	}
                }.start();
        	}
        }
        else
        {
        	a.runOnUiThread(new Runnable()
			{
				public void run()
				{
					if(location1.hasAccuracy())
					{
						mcr=mp.getController();
						mcr.animateTo(new GeoPoint((int)(location1.getLatitude()*1E6),(int)(location1.getLongitude()*1E6)));
						mp.invalidate();
					}
				}
			});
        }
        return true;
    }
    
    public boolean onTouchEvent(MotionEvent event, MapView map) 
    {   
        if(event.getAction()==1&&vv==0)
        {                
            new Thread()
            {
            	public void run()
            	{
            		GeoPoint p=mp.getMapCenter();
       				list=getPotHoleList(((double)p.getLatitudeE6())/(double)1E6,((double)p.getLongitudeE6())/(double)1E6);
            	}
            }.start();
        }
        return false;
    }
    
    protected void onResume() 
    {
        super.onResume();
        sensorManager.registerListener(sel,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_GAME);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, locationListener); 
    }

    protected void onPause() 
    {
        super.onPause();
        locationManager.removeUpdates(locationListener);
        sensorManager.unregisterListener(sel);
    }
    
    protected boolean isRouteDisplayed()
    {
    	return false;
    }
}