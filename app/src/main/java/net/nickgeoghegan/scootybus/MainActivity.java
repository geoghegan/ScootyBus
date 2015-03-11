package net.nickgeoghegan.scootybus;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.view.View;
import android.app.ListActivity;
import android.widget.ListView;
import android.content.DialogInterface;




import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class MainActivity extends ActionBarActivity
{

    /**
     * Intent Request Codes
     */
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_ENABLE_DISCOVERABILITY = 2;

    /**
     * Return Intent extra
     */
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    /**
     * Sets the tag for logging
     */
    private static final String TAG = "ScootyBus";


    /**
     * Gets the device's default Bluetooth Adapter
     */
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    /**
     * A set of bonded / already paired devices
     */
    private Set<BluetoothDevice> pairedDevices;

    /**
     * A generic listView
     */
    private ListView listView;


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

    /**
     * TODO: Remove the connection button altogether and init the BT interface on startup if it's not already enabled.
     */

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

       /* *//**
     * Enables bluetooth discovery, if it's not already enabled.
     * TODO: Verify that this is actually needed in real life if we have already paired with a device
     *//*
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
        }*/

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

    /**
     * Show a list of paired devices
     * TODO: Get rid of the button and just show a clickable list on startup
     */
    public void showDevices(View view)
    {

        Log.d(TAG, "In showDevices()");
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        listView = (ListView) findViewById(R.id.listView);
        ArrayList list = new ArrayList();

        for (BluetoothDevice device : pairedDevices)
        {
            list.add(device.getName() + "\n" + device.getAddress());

        }

        listView = (ListView) findViewById(R.id.listView);
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(mDeviceClickListener);

        if (list.isEmpty() && mBluetoothAdapter.isEnabled())
        {
            Toast.makeText(getApplicationContext(), "No Paired Devices", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "No Paired Devices");

        }
        else
        {
            Toast.makeText(getApplicationContext(), "Showing Paired Devices", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Showing Paired Devices");

        }

        Log.d(TAG, "Finished showDevices()");

    }

    /**
     * Selects a bluetooth device from the list
     */
    private AdapterView.OnItemClickListener mDeviceClickListener  = new AdapterView.OnItemClickListener()
    {

        /**
         *
         * @param foo -- meaningless, can be anything
         * @param view -- Only used to init the TextView
         * @param bar -- meaningless, can be anything
         * @param baz -- meaningless, can be anything
         */
        public void onItemClick(AdapterView<?> foo , View view, int bar, long baz)
        {

            Log.d(TAG, "In OnItemClick()");

            /**
             * Get the device MAC address, which is the last 17 chars in the View
             * TODO: Try to get rid of the 17 as a Magic Number
             */
            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);
            Log.d(TAG, "Selected MAC Address: " + address );

            /**
             * Creates the result Intent and includes the MAC address
             */
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            /**
             * Sets the result
             */
            setResult(Activity.RESULT_OK, intent);
            Log.d(TAG, "Finished OnItemClick()");

        }

    };

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

    /**
     * Clean up the bluetooth system when shutting down
     * Kinda negates the work done in onButtonCloseApp()
     */

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        /**
         * Disable discovery
         * TODO: Maybe just call this without the if statement
         */
        if (mBluetoothAdapter != null)
        {
            mBluetoothAdapter.cancelDiscovery();
        }

        /**
         * Disable bluetooth
         * TODO: Maybe just call this without the if statement
         */
        if (mBluetoothAdapter.isEnabled())
        {
            mBluetoothAdapter.disable();
        }

    }

    @Override
    public void onPause()
    {
        super.onPause();

        ; // noop
        /**
         * TODO: Release the bluetooth handler here
         */

    }

    @Override
    public void onResume()
    {
        super.onResume();

        ; // noop
        /**
         * TODO: Re-enable bluetooth and init the connection again
         */

    }

}