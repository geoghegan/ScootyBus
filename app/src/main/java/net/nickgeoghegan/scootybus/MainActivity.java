package net.nickgeoghegan.scootybus;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.widget.Toast;
import android.util.Log;
import android.view.View;


public class MainActivity extends ActionBarActivity
{

    /**
     * Intent Request Codes
     */
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_ENABLE_DISCOVERABILITY = 2;


    /**
     * Sets the tag for logging
     */
    private static final String TAG = "ScootyBus";


    /**
     * Gets the device's default Bluetooth Adapter
     */
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * Basic logging to see if we are in this function
         */
        Log.d(TAG, "In OnCreate()");

        /**
         * Sanity check to ensure the device actually has a bluetooth adapter
         */
        if (mBluetoothAdapter == null)
        {
            Toast.makeText(getApplicationContext(), "This device does not support bluetooth", Toast.LENGTH_LONG).show();
            Log.d(TAG, "This device does not support bluetooth... Requesting app to be killed");

            /**
             * Since this device does not have a bluetooth adapter, ask Android to kill the app
             */
            finish();
        }
    }

    public void onButtonBluetoothConnect(View view)
    {

        Log.d(TAG, "In onButtonBluetoothConnect()");

        /**
         * Enables the bluetooth adapter, if it's disabled, and sets the device into discovery mode
         */
        if (!mBluetoothAdapter.isEnabled())
        {
            Intent enableBlueTooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBlueTooth, REQUEST_ENABLE_BLUETOOTH);
            Toast.makeText(getApplicationContext(), "Enabling Bluetooth", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Enabling Bluetooth");
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Bluetooth is already enabled", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Bluetooth is already enabled");
        }

        /**
         * Enables bluetooth discovery, if it's not already enabled.
         * TODO: Verify that this is actually needed in real life if we have already paired with a device
         */
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
        {
            Intent enableDiscovery = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            startActivityForResult(enableDiscovery, REQUEST_ENABLE_DISCOVERABILITY);
            Toast.makeText(getApplicationContext(), "Enabling Discovery", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Enabling Discovery");
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Discovery is already enabled", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Discovery is already enabled");
        }

    }

    public void onButtonCloseApp(View view)
    {

        Log.d(TAG, "In onButtonCloseApp()");

        /**
         * Disables bluetooth
         */
        if (mBluetoothAdapter.isEnabled())
        {
            mBluetoothAdapter.disable();
            Toast.makeText(getApplicationContext(), "Disabling Bluetooth. Press again to close", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Disabling Bluetooth");
        }
        /**
         * Actually closes the app
         */
        else
        {
            Log.d(TAG, "Closing App");
            finish();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
