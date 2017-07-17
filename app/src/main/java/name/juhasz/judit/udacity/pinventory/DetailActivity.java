package name.juhasz.judit.udacity.pinventory;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;

import name.juhasz.judit.udacity.pinventory.data.PinventoryContract.PinventoryEntry;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_PINVENTORY_LOADER = 0;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private static final String LOG_TAG = DetailActivity.class.getSimpleName();

    private Uri mCurrentProductUri;

    private EditText mNameEditText;
    private EditText mPriceEditText;
    private EditText mQuantityEditText;
    private EditText mSupplierContactEditText;
    private ImageView mProductImageView;

    private boolean mProductHasChanged = false;

    private View.OnTouchListener mChangeTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        if (null == mCurrentProductUri) {
            setTitle(R.string.activity_add_product_label);
        } else {
            setTitle(R.string.activity_edit_product_label);
            getLoaderManager().initLoader(EXISTING_PINVENTORY_LOADER, null, this);
        }

        mNameEditText = (EditText) findViewById(R.id.et_product_name);
        mPriceEditText = (EditText) findViewById(R.id.et_product_price);
        mQuantityEditText = (EditText) findViewById(R.id.et_product_quantity);
        mSupplierContactEditText = (EditText) findViewById(R.id.et_supplier_contact);
        mProductImageView = (ImageView) findViewById(R.id.iv_product_image);

        mNameEditText.setOnTouchListener(mChangeTouchListener);
        mPriceEditText.setOnTouchListener(mChangeTouchListener);
        mQuantityEditText.setOnTouchListener(mChangeTouchListener);
        mSupplierContactEditText.setOnTouchListener(mChangeTouchListener);

        mProductImageView.setImageResource(R.drawable.ic_add_a_photo_black);
        mProductImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });
        mProductImageView.setTag(R.id.tag_real_image_added, false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (null == mCurrentProductUri) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                boolean saveSucceed = saveProduct();
                if (saveSucceed) {
                    finish();
                }
                return true;
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            case android.R.id.home:
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(DetailActivity.this);
                    return true;
                }

                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                NavUtils.navigateUpFromSameTask(DetailActivity.this);
                            }
                        };

                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            mProductImageView.setImageBitmap(imageBitmap);
            mProductImageView.setTag(R.id.tag_real_image_added, true);
            mProductHasChanged = true;
        }
    }

    private boolean saveProduct() {
        boolean productSaved = false;

        if (isValidProduct()) {
            ContentValues values = getProductContentValues();
            boolean isNewProduct = (null == mCurrentProductUri);
            if (isNewProduct) {
                Uri newUri = getContentResolver().insert(PinventoryEntry.CONTENT_URI, values);
                productSaved = (null != newUri);
                showToastIf(productSaved, R.string.editor_insert_product_successful,
                        R.string.editor_insert_product_failed);
            } else {
                int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);
                productSaved = (0 != rowsAffected);
                showToastIf(productSaved, R.string.editor_update_product_successful,
                        R.string.editor_update_product_failed);
            }
        } else {
            showToastMessage(R.string.editor_error_fields_blank);
        }

        return productSaved;
    }

    private void deleteProduct() {
        if (null != mCurrentProductUri) {
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);
            showToastIf(0 == rowsDeleted, R.string.editor_delete_product_failed,
                    R.string.editor_delete_product_success);
        }

        finish();
    }

    private boolean isValidProduct() {
        Object realImageAddedTag = mProductImageView.getTag(R.id.tag_real_image_added);
        boolean realImageAdded = (null != realImageAddedTag && (boolean) realImageAddedTag);

        return !TextUtils.isEmpty(mNameEditText.getText().toString().trim()) &&
                !TextUtils.isEmpty(mPriceEditText.getText().toString().trim()) &&
                !TextUtils.isEmpty(mQuantityEditText.getText().toString().trim()) &&
                !TextUtils.isEmpty(mSupplierContactEditText.getText().toString().trim()) &&
                realImageAdded;
    }

    private void showToastMessage(int messageResourceId) {
        Toast.makeText(DetailActivity.this, getString(messageResourceId),
                Toast.LENGTH_SHORT).show();
    }

    private void showToastIf(boolean condition, int successStringId, int failureStringId) {
        int messageResourceId = condition ? successStringId : failureStringId;
        showToastMessage(messageResourceId);
    }

    private ContentValues getProductContentValues() {
        ContentValues values = new ContentValues();

        String nameString = mNameEditText.getText().toString().trim();
        values.put(PinventoryEntry.COLUMN_PRODUCT_NAME, nameString);

        String priceString = mPriceEditText.getText().toString().trim();
        values.put(PinventoryEntry.COLUMN_PRODUCT_PRICE, priceString);

        String quantityString = mQuantityEditText.getText().toString().trim();
        values.put(PinventoryEntry.COLUMN_PRODUCT_QUANTITY, quantityString);

        String supplierContactString = mSupplierContactEditText.getText().toString().trim();
        values.put(PinventoryEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL, supplierContactString);

        BitmapDrawable drawable = (BitmapDrawable) mProductImageView.getDrawable();
        byte[] imageBytes = getBytes(drawable.getBitmap());
        values.put(PinventoryEntry.COLUMN_PRODUCT_IMAGE, imageBytes);

        return values;
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String[] projection = {
                PinventoryEntry._ID,
                PinventoryEntry.COLUMN_PRODUCT_NAME,
                PinventoryEntry.COLUMN_PRODUCT_PRICE,
                PinventoryEntry.COLUMN_PRODUCT_QUANTITY,
                PinventoryEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL,
                PinventoryEntry.COLUMN_PRODUCT_IMAGE };


        return new CursorLoader(this,
                mCurrentProductUri,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (null == cursor || 1 > cursor.getCount()) {
            return;
        }

        if (cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex(PinventoryEntry.COLUMN_PRODUCT_NAME);
            String name = cursor.getString(nameColumnIndex);
            mNameEditText.setText(name);

            int priceColumnIndex = cursor.getColumnIndex(PinventoryEntry.COLUMN_PRODUCT_PRICE);
            double price = cursor.getDouble(priceColumnIndex);
            mPriceEditText.setText(Double.toString(price));

            int quantityColumnIndex = cursor.getColumnIndex(PinventoryEntry.COLUMN_PRODUCT_QUANTITY);
            int quantity = cursor.getInt(quantityColumnIndex);
            mQuantityEditText.setText(Integer.toString(quantity));

            int supplierContactColumnIndex = cursor.getColumnIndex(PinventoryEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL);
            String supplierContact = cursor.getString(supplierContactColumnIndex);
            mSupplierContactEditText.setText(supplierContact);

            int imageColumnIndex = cursor.getColumnIndex(PinventoryEntry.COLUMN_PRODUCT_IMAGE);
            byte[] imageBytes = cursor.getBlob(imageColumnIndex);
            Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            mProductImageView.setImageBitmap(imageBitmap);
            mProductImageView.setTag(R.id.tag_real_image_added, true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
        mSupplierContactEditText.setText("");
    }

    @Override
    public void onBackPressed() {
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                };

        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.message_unsaved_changes_dialog);
        builder.setPositiveButton(R.string.option_discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.option_keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (null != dialog) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.message_delete_dialog);
        builder.setPositiveButton(R.string.option_delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.option_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (null != dialog) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void increaseQuantity(View v) {
        int quantity = 0;
        String quantityText = mQuantityEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(quantityText)) {
            quantity = Integer.parseInt(quantityText);
        }
        mQuantityEditText.setText(String.valueOf(quantity + 1));
        mProductHasChanged = true;
    }

    public void decreaseQuantity(View v) {
        int quantity = 0;
        String quantityText = mQuantityEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(quantityText)) {
            quantity = Integer.parseInt(quantityText);
        }
        if (0 < quantity) {
            mQuantityEditText.setText(String.valueOf(quantity - 1));
            mProductHasChanged = true;
        }
    }

    public void orderProduct(View view) {
        String nameString = mNameEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String supplierContactString = mSupplierContactEditText.getText().toString().trim();

        Intent sendEmailIntent = new Intent(Intent.ACTION_SENDTO,
                Uri.fromParts("mailto",supplierContactString, null));
        sendEmailIntent.putExtra(Intent.EXTRA_SUBJECT, R.string.email_order_subject);
        sendEmailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_order_message, nameString));

        if (null != sendEmailIntent.resolveActivity(getPackageManager())) {
            startActivity(sendEmailIntent);
        } else {
            Log.w(LOG_TAG, "Install a browser to read the article.");
            showToastMessage(R.string.error_no_browser);
        }
    }
}
