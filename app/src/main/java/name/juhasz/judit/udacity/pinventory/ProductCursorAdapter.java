package name.juhasz.judit.udacity.pinventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import name.juhasz.judit.udacity.pinventory.data.PinventoryContract.PinventoryEntry;

public class ProductCursorAdapter extends CursorAdapter {

    private Context mContext;

    public ProductCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
        mContext = context;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        TextView nameTextView = (TextView) view.findViewById(R.id.tv_name);
        int nameColumnIndex = cursor.getColumnIndex(PinventoryEntry.COLUMN_PRODUCT_NAME);
        String productName = cursor.getString(nameColumnIndex);
        nameTextView.setText(productName);

        TextView priceTextView = (TextView) view.findViewById(R.id.tv_price);
        int priceColumnIndex = cursor.getColumnIndex(PinventoryEntry.COLUMN_PRODUCT_PRICE);
        int productPrice = cursor.getInt(priceColumnIndex);
        priceTextView.setText(String.valueOf(productPrice));

        final TextView quantityTextView = (TextView) view.findViewById(R.id.tv_quantity);
        int quantityColumnIndex = cursor.getColumnIndex(PinventoryEntry.COLUMN_PRODUCT_QUANTITY);
        int productQuantity = cursor.getInt(quantityColumnIndex);
        quantityTextView.setText(String.valueOf(productQuantity));

        final int idColumnIndex = cursor.getColumnIndex(PinventoryEntry._ID);
        final int productId = cursor.getInt(idColumnIndex);

        view.setOnClickListener(openProductDetailsAction(productId));

        Button saleProductButton = (Button) view.findViewById(R.id.b_sale);
        saleProductButton.setOnClickListener(sellOneItemAction(quantityTextView, productId));
    }

    private View.OnClickListener openProductDetailsAction(final int productId) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, DetailActivity.class);
                Uri contentProductUri = ContentUris.withAppendedId(PinventoryEntry.CONTENT_URI, productId);
                intent.setData(contentProductUri);
                mContext.startActivity(intent);
            }
        };
    }

    private View.OnClickListener sellOneItemAction(final TextView quantityTextView, final int productId) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int quantity = Integer.parseInt(quantityTextView.getText().toString());

                if (0 == quantity) {
                    Toast.makeText(mContext, R.string.message_no_more_in_stock,
                            Toast.LENGTH_SHORT).show();
                } else if (quantity > 0) {
                    ContentValues values = new ContentValues();
                    values.put(PinventoryEntry.COLUMN_PRODUCT_QUANTITY, quantity-1);

                    Uri currentProductUri =
                            ContentUris.withAppendedId(PinventoryEntry.CONTENT_URI, productId);

                    int rowsAffected =
                            mContext.getContentResolver().update(currentProductUri, values, null, null);

                    if (0 != rowsAffected) {
                        quantityTextView.setText(Integer.toString(quantity-1));
                    } else {
                        Toast.makeText(mContext, R.string.message_error_with_update, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
    }
}
