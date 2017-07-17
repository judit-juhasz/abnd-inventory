package name.juhasz.judit.udacity.pinventory;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.io.ByteArrayOutputStream;

import name.juhasz.judit.udacity.pinventory.data.PinventoryContract.PinventoryEntry;

public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int PRODUCT_LOADER = 0;

    private ProductCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_add_new_product);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, DetailActivity.class);
                startActivity(intent);
            }
        });

        ListView productListView = (ListView) findViewById(R.id.lv_products);

        View emptyView = findViewById(R.id.empty_view);
        productListView.setEmptyView(emptyView);

        mCursorAdapter = new ProductCursorAdapter(this, null);
        productListView.setAdapter(mCursorAdapter);

        getLoaderManager().initLoader(PRODUCT_LOADER, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_insert_dummy_data:
                insertProduct();
                return true;
            case R.id.action_delete_all_entries:
                deleteAllProduct();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                PinventoryEntry._ID,
                PinventoryEntry.COLUMN_PRODUCT_NAME,
                PinventoryEntry.COLUMN_PRODUCT_PRICE,
                PinventoryEntry.COLUMN_PRODUCT_QUANTITY,
        };
        return new CursorLoader(this,
                PinventoryEntry.CONTENT_URI,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    private void insertProduct() {
        ContentValues values = new ContentValues();

        values.put(PinventoryEntry.COLUMN_PRODUCT_NAME, "Phone");
        values.put(PinventoryEntry.COLUMN_PRODUCT_PRICE, 120);
        values.put(PinventoryEntry.COLUMN_PRODUCT_QUANTITY, 5);
        values.put(PinventoryEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL, "judit@juhasz.name");
        BitmapDrawable drawable = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            drawable = (BitmapDrawable) getDrawable(R.drawable.image_empty_shelter);
        }
        byte[] imageBytes = getBytes(drawable.getBitmap());
        values.put(PinventoryEntry.COLUMN_PRODUCT_IMAGE, imageBytes);

        getContentResolver().insert(PinventoryEntry.CONTENT_URI, values);
    }

    private static byte[] getBytes(Bitmap bitmap) {
        if (bitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
            return stream.toByteArray();
        } else {
            return null;
        }
    }

    private void deleteAllProduct() {
        int rowsDeleted = getContentResolver().delete(PinventoryEntry.CONTENT_URI, null, null);
        Log.v("CatalogActivity", rowsDeleted + " rows deleted from pet database");
    }
}
