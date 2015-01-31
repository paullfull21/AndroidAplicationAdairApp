package com.example.adairapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;
import com.example.adairapp.R;

/*
 * @author Espinoza Alcala Paul M.
 */

public class MainActivity extends android.support.v4.app.FragmentActivity {
	
	//variable to access fragment
	GoogleMap map;
	//initial position of the map
	static final LatLng bahiaAdair = new LatLng(31.5292098, -113.7109934);
	
	HashMap<Marker, String> cacheMapPaths = new HashMap<Marker, String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		map = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map)).getMap();

		map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
		map.setMyLocationEnabled(true);
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(bahiaAdair, 12));
		
		//info Window Adapter
		map.setInfoWindowAdapter(new InfoWindowAdapter() { 
			
	        @Override
	        public View getInfoContents(Marker marker) {
	        	View myContentsView = getLayoutInflater().inflate(R.layout.custom_info_window, null);
	        	
	            TextView tvTitle = ((TextView)myContentsView.findViewById(R.id.tvTitle));
	            tvTitle.setText(marker.getTitle());
	            
	            TextView tvSnippet = ((TextView)myContentsView.findViewById(R.id.tvSnippet));
	            tvSnippet.setText(marker.getSnippet());
	            
	            //ImageView ivImage = (ImageView)findViewById(R.id.ivImage);
	            ImageView ivImage = (ImageView)myContentsView.findViewById(R.id.ivImage);
	            String mapPath = cacheMapPaths.get(marker);
	            Picasso.with(MainActivity.this).load(mapPath).into(ivImage);
	    		//Bitmap foto = procesarImagePath(mapPath);
	            

	            return myContentsView;
	        	
	        }
	        @Override
	        public View getInfoWindow(Marker marker) {
				return null;
	        }
	    });
		/*map.setOnMarkerClickListener(new OnMarkerClickListener() {
			@Override
			public boolean onMarkerClick(Marker marker) {
				return true;
			}
			
		});*/
		
        new RetrieveTask().execute();
	}

	    
	// Adding marker on the GoogleMaps
    private void addMarker(LatLng latlng, String title, String snippet, String imagePath) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latlng);
        markerOptions.title(title);
		markerOptions.snippet(snippet);
		
		
		//map.addMarker(markerOptions);
        Marker nuevoMarcador = map.addMarker(markerOptions);
        cacheMapPaths.put(nuevoMarcador,imagePath);     
    }
    
    // Background task to retrieve locations from remote mysql server
    private class RetrieveTask extends AsyncTask<Void, Void, String>{
 
        @Override
        protected String doInBackground(Void... params) {
            String strUrl = "http://192.168.1.81/dbAdairApp/retrieve.php";
            URL url = null;
            StringBuffer sb = new StringBuffer();
            try {
                url = new URL(strUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream iStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(iStream));
                String line = "";
                while( (line = reader.readLine()) != null){
                    sb.append(line);
                }
 
                reader.close();
                iStream.close();
 
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return sb.toString();
        }
 
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            new ParserTask().execute(result);
        }
    }
    
    // Background thread to parse the JSON data retrieved from MySQL server
    private class ParserTask extends AsyncTask<String, Void, List<HashMap<String, String>>>{
        @Override
        protected List<HashMap<String,String>> doInBackground(String... params) {
            JSONParser markerParser = new JSONParser();
            JSONObject json = null;
            try {
                json = new JSONObject(params[0]);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            List<HashMap<String, String>> markersList = markerParser.parse(json);
            return markersList;
        }
 
        @Override
        protected void onPostExecute(List<HashMap<String, String>> result) {
            for(int i=0; i<result.size();i++){
                HashMap<String, String> marker = result.get(i);
                LatLng latlng = new LatLng(Double.parseDouble(marker.get("lat")), Double.parseDouble(marker.get("lng")));
                String title = marker.get("title");
                String snippet = marker.get("snippet");
                String imagePath = marker.get("image");
                
                addMarker(latlng, title, snippet, imagePath);
            }
        }        
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		
		return true;		
	}
	
}
	
	
