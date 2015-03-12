package net.nickgeoghegan.scootybus;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
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


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends ActionBarActivity
{

    /**
     * Intent Request Codes
     */
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_ENABLE_DISCOVERABILITY = 2;

    /**
     * Intent to get the devices address
     */
    public String REMOTE_DEVICE_ADDRESS = "device_address";

    /**
     * Sets the standard Serial Port Profile (SPP) UUID
     */
    private static final UUID uuidSPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * Sets the socket
     */
    private BluetoothSocket mBluetoothSocket = null;

    /**
     * Sets the input and output streams
     */
    private OutputStream outputStream = null;
    private InputStream inputStream = null;


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

        Log.d(TAG, "Finished OnCreate()");

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

        Log.d(TAG, "Finished onButtonBluetoothConnect()");

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
     * Sends an ATI to the ELM327 chipset and checks it's return value
     */
    public void onSendATI(View view)
    {
        /**
         * TODO: Actually get this working as a real, non-testing, button
         */
        Log.d(TAG, "In onSendATI");
        onConnect();
        sendData("ATI");
        Log.d(TAG, "Finished onSendATI");

    }

    /**
     * Connects to a device
     */
    public void onConnect()
    {

        Log.d(TAG, "In onConnect()");

        Log.d(TAG, "Remote device's MAC is: " + REMOTE_DEVICE_ADDRESS);

        /**
         * Creates a pointer to the remote device using it's MAC address
         */
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(REMOTE_DEVICE_ADDRESS);

        /**
         * Creates the socket
         */
        try
        {
            Log.d(TAG, "Attempting to create mBluetoothSocket");
            mBluetoothSocket = device.createRfcommSocketToServiceRecord(uuidSPP);
        }
        catch (IOException e)
        {
            Log.d(TAG, "FATAL: In onConnect() and socket create failed: " + e.getMessage() + ".");
        }

        /**
         * Create the connection
         * This is a blocking operation!
         */
        Log.d(TAG, "Attempting to connect to remote device: " + REMOTE_DEVICE_ADDRESS);

        try
        {
            mBluetoothSocket.connect();
            Log.d(TAG, "Connected to remote device: " + REMOTE_DEVICE_ADDRESS);
        }
        catch (IOException e)
        {
            try
            {
                mBluetoothSocket.close();
                Log.d(TAG, "Caught remote device connection failure: " + e.getMessage() + ".");
            }
            catch (IOException f)
            {
                Log.d(TAG, "FATAL: In onConnect() and unable to close socket during connection failure: " + f.getMessage() + ".");
            }
        }

        Log.d(TAG, "Finished onConnect()");

    }

    /**
     * Sends arbitrary data to the remote device
     */
    public void sendData(String rawMessage)
    {

        Log.d(TAG, "In sendData()");

        /**
         * The ELM 327 needs a return and newline to run commands
         */
        String ELM_TERMINATOR = "\r\n";

        /**
         * Saves us having to specify the terminator for every command sent
         */
        String message = rawMessage + ELM_TERMINATOR;

        /**
         * Creates the messageBuffer, which is what we'll actually write to the outputStream
         */
        byte[] messageBuffer = message.getBytes();

        /**
         * Sends the data
         */
        Log.d(TAG, "Attempting to send: " + message + ".");
        try
        {
            outputStream.write(messageBuffer);
            Log.d(TAG, "Sent: " + message);
        }
        catch(IOException e)
        {
            Log.d(TAG, "FATAL: Exception occurred when attempting to send: " + message );
        }

        Log.d(TAG, "Finished SendData()");

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
        Toast.makeText(getApplicationContext(), "No Paired Devices or Bluetooth is disabled", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "No Paired Devices or Bluetooth is disabled");

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
             * Sets the remote device address
             */
            REMOTE_DEVICE_ADDRESS = address;

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
        Log.d(TAG, "In onDestroy()");

        super.onDestroy();

        /**
         * Disables discovery & bluetooth
         */
        mBluetoothAdapter.cancelDiscovery();
        mBluetoothAdapter.disable();

        Log.d(TAG, "Finished onDestroy()");

    }

    @Override
    public void onPause()
    {

        Log.d(TAG, "In onPause()");

        super.onPause();

        /**
         * TODO: Release the bluetooth handler here
         */

        /**
         * Bluetooth discovery is a heavyweight task that kills battery
         */
        mBluetoothAdapter.cancelDiscovery();

        /**
         * Disable bluetooth on pause to save battery (less important than discovery)
         */
        mBluetoothAdapter.disable();

        Log.d(TAG, "Finished onPause()");
    }

    @Override
    public void onResume()
    {
        Log.d(TAG, "In onResume()");

        super.onResume();

        /**
         * If bluetooth was disabled when we were asleep, re-enable it
         */
        if (!mBluetoothAdapter.isEnabled())
        {
            mBluetoothAdapter.enable();
        }

        Log.d(TAG, "Finished onResume()");

    }

}