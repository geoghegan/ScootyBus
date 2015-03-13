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


public class MainActivity extends Activity
{

    /**
     * State to track if we're connected
     */
    public static final int STATE_NOT_CONNECTED = 3;
    public static final int STATE_CONNECTED = 6;
    private int mState;


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

        /**
         * Turn the bluetooth adaptor on when the app is started
         */
        mBluetoothAdapter.enable();

        /**
         * Show the list of already paired devices at startup
         *
         * The noop / while loop combo is there as a spinner to populate the
         * device list on app startup if the adaptor isn't fully turned on.
         * TODO: Make the spinner a bit more elegant
         * TODO: Replace with self made Adapter states
         * TODO: This will help with crashing on sendData when not connected
         */
        while(mBluetoothAdapter.getState() !=  BluetoothAdapter.STATE_ON )
        {
            ; // noop
        }
        showDevices();

        Log.d(TAG, "Finished OnCreate()");

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
     * Sends an ATI to the ELM327 chipset
     */
    public void onSendATI(View view)
    {

        Log.d(TAG, "In onSendATI");

        /**
         * App gets killed by Android if it tries to sendData without the socket being ready
         */
        if(mState == STATE_CONNECTED)
        {
            sendData("ATI");
            Log.d(TAG, "STATE_CONNECTED: calling sendData(ATI)");
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Please connect to a device first!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Tried to sendData - can't as not connected");
        }

        Log.d(TAG, "Finished onSendATI");

    }

    /**
     * Lowers Windows
     */
    public void onSendLowerWindows(View view)
    {

        Log.d(TAG, "In onSendLowerWindows");

        sendData("ATSP5");
        sendData("ATWM 81 BC F0 81");
        sendData("ATSH 83 BC F0");
        sendData("30 03 01");

        Log.d(TAG, "Finished onSendLowerWindows");

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
            mBluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(uuidSPP);
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
            Toast.makeText(getApplicationContext(), "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();


            /**
             * Sets the connection state
             */
            mState = STATE_CONNECTED;
            Log.d(TAG, "Setting STATE_CONNECTED");
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
        Log.d(TAG, "Attempting to send: " + rawMessage + ".");
        try
        {
            /**
             * Create output stream
             */
            Log.d(TAG, "Getting outputStream");
            OutputStream outputStream = mBluetoothSocket.getOutputStream();
            Log.d(TAG, "Got outputStream");

            /**
             * Actually writes the message
             */
            Log.d(TAG, "Attempting to write to outputStream");
            outputStream.write(messageBuffer);
            Log.d(TAG, "Sent: " + message);

            /**
             * Might as well flush
             */
            outputStream.flush();
            Log.d(TAG, "Output stream flushed!");

        }
        catch(IOException e)
        {
            Log.d(TAG, "FATAL: Exception occurred when attempting to send: " + message );
        }

        Log.d(TAG, "Finished SendData()");

    }

    /**
     * Show a list of paired devices
     */
    public void showDevices()
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

        if (list.isEmpty())
        {
            Toast.makeText(getApplicationContext(), "No Paired Devices or Bluetooth is disabled", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "No Paired Devices or Bluetooth is disabled");

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
            onConnect();

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

    /**
     * What to do then the app looses focus
     */
    @Override
    public void onPause()
    {

        super.onPause();

        Log.d(TAG, "In onPause()");

        ; //noop

        Log.d(TAG, "Finished onPause()");

    }

    /**
     * What to dow when the app regains focus
     * TODO: Reconnect to the remote device.
     */
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