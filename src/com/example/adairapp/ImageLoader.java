package com.example.adairapp;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;


public class ImageLoader {
	private Context context;
	private LruCache<String, Bitmap> cache;
	
	public ImageLoader(Context context) {
		this.context = context;
		
		final int maxMemory = (int)(Runtime.getRuntime().maxMemory() / 1024); // Límite de memoria convertido a KiloBytes
		final int cacheMemory = maxMemory / 8; // Una octava parte de la memoria total será caché, debería bastar...
		cache = new LruCache<String, Bitmap>(cacheMemory) {
			protected int sizeOf(String key, Bitmap value) {
				return value.getByteCount() / 1024;
			}
		};
	}
	
	// Habría que buscar la forma de cargar la imagen en diferentes medidas
	public void displayImage(String url, ImageView targetView) {
		BitmapWorkerTask task = new BitmapWorkerTask(targetView, cache);
		task.execute(url);
	}
	
	/**
	 * Descodifica una url de imagen y devuelve el objeto Bitmap correspondiente,
	 * la imagen se reescalará conservando el ratio de aspecto para ahorrar memoria.
	 * @param url Url de la imágen objetivo
	 * @param targetWidth El ancho deseado para la imagen resultante
	 * @param targetHeight El alto deseado para la imagen resultante
	 * @return Un Bitmap con los datos de la imagen
	 */
	public static Bitmap decodeImageFromUrl(String url, Integer targetWidth, Integer targetHeight) {
		
		Bitmap mBitmap = null;
		final BitmapFactory.Options options = new BitmapFactory.Options();
		
		try {
			URL mImageUrl = new URL(url);
			InputStream is = mImageUrl.openStream();
			
			options.inJustDecodeBounds = true;
			mBitmap = BitmapFactory.decodeStream(mImageUrl.openStream());
			
			options.inSampleSize = calculateInSampleSize(mBitmap.getWidth(), mBitmap.getHeight(), targetWidth, targetHeight);
			
			options.inJustDecodeBounds = false;
			mBitmap = BitmapFactory.decodeStream(is, null, options);
			return mBitmap;
		} catch (Exception ex) {
			Log.e("Error en ImageLoader", ex.getMessage());
			return null;
		}
		
	}
	
	public static Bitmap decodeSampleImageFromResources(Resources res, Integer resId, 
			Integer targetWidth, Integer targetHeight) {
		
		final BitmapFactory.Options options = new BitmapFactory.Options();
		
		// No devuelve una imagen pero mantiene sus parámetros de salida,
		// se usa ahora para poder calcular el valor de reescalado de la imagen
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId);
		
		options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight);
		
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, options);
		
	}
	
	// ALGO ESTÁ FALLANDO AL HACER EL CÁLCULO
	public static int calculateInSampleSize(
			BitmapFactory.Options options, Integer targetWidth, Integer targetHeight) {
		final int height = options.outHeight; // Para acceder a estos valores se usa el inJustDecodeBounds = true más arriba
		final int width = options.outWidth;
		int inSampleSize = 8; // Valor por defecto para el valor de escalado
		
		Log.i("Alto salida", String.valueOf(height));
		Log.i("Width salida", String.valueOf(width));
		
		if (targetWidth == 0 || targetHeight == 0) return inSampleSize;
		
		if (height > targetHeight || width > targetWidth) {
			
			// Calculas los radios para la altura y anchura, luego te quedas con la menor
			final int widthRadio = Math.round((float)width / (float)targetWidth);
			final int heightRadio = Math.round((float)height / (float)targetHeight);
			
			inSampleSize = widthRadio < heightRadio ? widthRadio : heightRadio;	
		}
		return inSampleSize;
	}
	
	/**
	 * Calcula el valor que se usará para el reescalado de la imagen. Se calculan
	 * los ratios horizontal y vertical y se usa el menor de ellos para el reescalado,
	 * si alguna de las medidas deseadas es igual a 0 se usa un valor de reescalado
	 * predeterminado.
	 * @param outWidth El ancho original de la imagen
	 * @param outHeight El alto original de la imagen
	 * @param targetWidth El ancho deseado para la imagen resultante
	 * @param targetHeight El alto deseado para la imagen resultanto
	 * @return Valor que sirve para el reescalado de la imagen
	 */
	public static int calculateInSampleSize(
		int outWidth, int outHeight, Integer targetWidth, Integer targetHeight) {
		final int height = outHeight;
		final int width = outWidth;
		int inSampleSize = 16; // Valor por defecto para el valor de escalado
		
		Log.i("Alto salida", String.valueOf(height));
		Log.i("Width salida", String.valueOf(width));
		
		if (targetWidth == 0 || targetHeight == 0) return inSampleSize;
		
		if (height > targetHeight || width > targetWidth) {
			
			// Calculas los radios para la altura y anchura, luego te quedas con la menor
			final int widthRadio = Math.round((float)width / (float)targetWidth);
			final int heightRadio = Math.round((float)height / (float)targetHeight);
			
			return (widthRadio < heightRadio) ? widthRadio : heightRadio;	
		}
		return inSampleSize;
	}
	
	/**
	 * AsyncTask que se encarga de descargar, reescalar, y guardar en
	 * memoria una imagen de internet en un hilo separado del de la UI.
	 * @author Gabriel Ferreiro Blazetic.
	 *
	 */
	private class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

		private LruCache<String, Bitmap> cache;
		private final WeakReference<ImageView> imageViewReference;
		private String resUrl;
		
		public BitmapWorkerTask(ImageView targetView, LruCache<String, Bitmap> cache) {
			this.imageViewReference = new WeakReference<ImageView>(targetView);
			this.cache = cache;
		}
		
		@Override
		protected Bitmap doInBackground(String... params) {
			resUrl = params[0];
			
			if (cache.get(resUrl) != null)
					return cache.get(resUrl);
			
			// TODO: Buscar la forma de poder dinamizar el tamaño deseado de la imagen
			Bitmap bitmap = decodeImageFromUrl(resUrl, 120, 120);
			cache.put(resUrl, bitmap);
			return bitmap;
		}
		
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (isCancelled()) {
				bitmap = null;
			}
			if (imageViewReference != null && bitmap != null) {
				final ImageView targetView = imageViewReference.get();
				if (targetView != null)
					targetView.setImageBitmap(bitmap);
			}
		}	
		
	}
}