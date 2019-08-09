/*
 * Copyright 2017, Cypress Semiconductor Corporation or a subsidiary of Cypress Semiconductor
 * Corporation. All rights reserved. This software, including source code, documentation and
 * related materials ("Software"), is owned by Cypress Semiconductor  Corporation or one of its
 * subsidiaries ("Cypress") and is protected by and subject to worldwide patent protection 
 * (United States and foreign), United States copyright laws and international treaty provisions.
 * Therefore, you may use this Software only as provided in the license agreement accompanying the
 * software package from which you obtained this Software ("EULA"). If no EULA applies, Cypress
 * hereby grants you a personal, nonexclusive, non-transferable license to  copy, modify, and
 * compile the Software source code solely for use in connection with Cypress's  integrated circuit
 * products. Any reproduction, modification, translation, compilation,  or representation of this
 * Software except as specified above is prohibited without the express written permission of
 * Cypress. Disclaimer: THIS SOFTWARE IS PROVIDED AS-IS, WITH NO  WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING,  BUT NOT LIMITED TO, NONINFRINGEMENT, IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE. Cypress reserves the right to make changes to
 * the Software without notice. Cypress does not assume any liability arising out of the application
 * or use of the Software or any product or circuit  described in the Software. Cypress does
 * not authorize its products for use in any products where a malfunction or failure of the
 * Cypress product may reasonably be expected to result  in significant property damage, injury
 * or death ("High Risk Product"). By including Cypress's product in a High Risk Product, the
 * manufacturer of such system or application assumes  all risk of such use and in doing so agrees
 * to indemnify Cypress against all liability.
 */
package com.cypress.le.mesh.meshapp;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.cypress.le.mesh.meshapp.Adapter.NodeAdapter;
import com.cypress.le.mesh.meshcore.MeshNativeHelper;
import com.cypress.le.mesh.meshframework.IMeshControllerCallback;
import com.cypress.le.mesh.meshframework.MeshController;
import com.larswerkman.holocolorpicker.ColorPicker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.cypress.le.mesh.meshapp.Adapter.GrpDeviceListAdapter;

public class ActivityGroup extends AppCompatActivity implements LightingService.IServiceCallback {
    private static final String TAG = "ActivityGroup";

    public static final int LIGHT_NOT_ADDED = 0;
    public static final int LIGHT_ADDED = 1;
    public static final int PROVISION_SPINNER_TIMEOUT = 45000;
    ImageView imageView;
    Drawable image = null;
    Toolbar toolbar;

    View proxyConnView;
    //Additional variables for button to get all sensor values and to display them
    Button getSensorValue,showSensorValue;
    String groupName;
    String name;
    String type = null;
    Uri tempUri;
    private File file;
    MeshBluetoothDevice mPeerDevice = null;
    private static Toast mToast = null;
    static int propertySelected;

    //    ArrayList<String> groups= new ArrayList<String>();
    ArrayList<String> components = new ArrayList<String>();
    ArrayList<Integer> componentType = new ArrayList<Integer>();
    GrpDeviceListAdapter adapterGrplist;
    private ArrayAdapter<MeshBluetoothDevice> peerDevices;
    private static MeshApp mApp;
    private String newNode;
    private GrpDeviceListAdapter adapter;
    private final int  RESULT_LOAD_IMG = 1;
    private final int PIC_CROP = 2;
    private int mLightAddedStatus = LIGHT_NOT_ADDED;
    ExpandableHeightListView lv;
    CollapsingToolbarLayout collapsingToolbarLayout;
    static LightingService serviceReference = null;
    //TextView transtime = null;
    static int mColor = 0;
    int time = 0;
    // ProgressDialog progress;
    boolean isConfigComplete = true;
    boolean isStoppedScan = false;
    boolean startProvision = false;
    String mDeviceName = "DEVICE";
    static Semaphore semaphore = new Semaphore(1);
    Handler mSpinTimer = new Handler();

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        if(components.size()!= 0 ) {
            components = new ArrayList<String>(Arrays.asList(serviceReference.getMesh().getGroupComponents(groupName)));
            Log.d(TAG, "Inside if statement");

            componentType = new ArrayList<Integer>();
            for(int i=0; i< components.size(); i++) {
                componentType.add(serviceReference.getMesh().getComponentType(components.get(i)));
            }
            Log.d(TAG, "Components "+components);

            adapterGrplist = new GrpDeviceListAdapter(serviceReference, ActivityGroup.this, components, componentType, null, "group", groupName);
            if(adapterGrplist!=null)
                adapterGrplist.notifyDataSetChanged();
            lv.setAdapter(adapterGrplist);

        }
        if(serviceReference != null) {
            serviceReference.registerCb(ActivityGroup.this);
            Log.d(TAG, "Components1 "+components);

        }

        runOnUiThread(new Runnable() {

            public void run() {
                if(mApp.getMesh().isConnectedToNetwork()) {
                    if(mApp.getPreferredTransport()==Constants.TRANSPORT_GATEWAY)
                        proxyConnView.setBackgroundColor(getResources().getColor(R.color.blue));
                    else
                        proxyConnView.setBackgroundColor(getResources().getColor(R.color.green));
                } else {
                    proxyConnView.setBackgroundColor(getResources().getColor(R.color.red));
                }
            }
        });
    }


    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        unregisterReceiver(mReceiver);
        unbindService(mConnection);
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);
        Intent serviceIntent= new Intent(this, LightingService.class);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        Intent intent = getIntent();
        Log.d(TAG, "onCreate");
        try {
            createExcel();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(intent!=null) {
            Bundle extras = getIntent().getExtras();
            if(extras != null) {
                type = extras.getString("groupType");
                name = extras.getString("name");
                groupName = extras.getString("GroupName");
                Log.d(TAG, "name =" + name + " mId=" + groupName + " type=" + type);
            } else {
                Log.d(TAG, "Extras are null");
            }
        } else {
            Log.d(TAG, "Intent is null");
        }

        mApp = (MeshApp) getApplication();
        toolbar = (Toolbar) findViewById(R.id.toolbar);

     //   FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        toolbar.setTitle(name);
        toolbar.inflateMenu(R.menu.menu_main);
        setSupportActionBar(toolbar);
        Button lightset = (Button) findViewById(R.id.lightSet);
      //  Button config = (Button) findViewById(R.id.config);
        Button connectToProxy = (Button) findViewById(R.id.connectToProxy);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        imageView = (ImageView)findViewById(R.id.bgheader);
        proxyConnView = (View)findViewById(R.id.proxy);
        newNode = null;
        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);

        if(type.equals("room")) {
            if(image != null) {

                if(image != null) {
                    imageView.setBackground(image);
                }
            }
        } else {
           // fab.setVisibility(View.INVISIBLE);
            imageView.setBackgroundColor(getResources().getColor(R.color.primary));
            lightset.setVisibility(View.VISIBLE);

        }


//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent galleryIntent = new Intent(Intent.ACTION_PICK,
//                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
//
//            }
//        });
        connectToProxy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"Connect to Network");
                AlertDialog alertDialog = new AlertDialog.Builder(ActivityGroup.this).create();
                alertDialog.setTitle("Connect To Network");
                alertDialog.setMessage("\nConnect to Mesh Network ?");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                if(!isLocationServiceEnabled()) {
                                    Log.d(TAG, "isLocationServiceEnabled : location is false");
                                    show("Please turn on the location!!!", Toast.LENGTH_SHORT);
                                }
                                else {
                                    Log.d(TAG, "isLocationServiceEnabled : location is true");
                                    if(mApp.connectToNetwork(Constants.TRANSPORT_GATT) == MeshController.MESH_CLIENT_SUCCESS)
                                    {
                                        proxyConnView.setBackgroundColor(getResources().getColor(R.color.red));
                                        show("Connecting...", Toast.LENGTH_SHORT);
                                    }

                                }

                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();

            }
        });


        // Set an OnMenuItemClickListener to handle menu item clicks
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Handle the menu item
                Log.d(TAG, "home" + item.getItemId());
                switch (item.getItemId()) {
                    case R.id.edit:
                        Log.d(TAG, "edit");
                        View editView = View.inflate(ActivityGroup.this, R.layout.pop_up_edit_name, null);
                        popUpEdit(editView);
                        break;
                    case R.id.delete:
                        if (mApp.getPreferredTransport() == Constants.TRANSPORT_GATT) {
                            View deleteView = View.inflate(ActivityGroup.this, R.layout.pop_up_delete, null);
                            popUpDelete(deleteView);
                        } else {
                            show("Feature not supported in away mode", Toast.LENGTH_SHORT);
                        }
                        break;
                    case R.id.refresh:
                        updateDisplay();
                        break;
                    case R.id.disconnect:
                        AlertDialog alertDialog = new AlertDialog.Builder(ActivityGroup.this).create();
                        alertDialog.setTitle("Disconnect Network");
                        alertDialog.setMessage("\nDo you want to disconnect current Network ?");
                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        if (serviceReference.getMesh().isConnectedToNetwork()) {
                                            serviceReference.getMesh().disconnectNetwork();
                                        } else {
                                            show("Network is not connected!!!", Toast.LENGTH_SHORT);
                                        }
                                    }
                                });
                        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.show();

                        break;
                }
                return true;
            }
        });
        findViewById(R.id.lightSet).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(type.equals("room")) {
                    View groupView = View.inflate(ActivityGroup.this, R.layout.pop_up_new_group, null);
                    //popUpNewGroup(groupView);
                }
            }
        });
        final ImageButton imgbtn = (ImageButton) findViewById(R.id.seekBar5);

        if(imgbtn != null) {
            imgbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    View lightView = View.inflate(ActivityGroup.this,R.layout.pop_up_setting_color, null);
                    popUpLight(lightView);
                }
            });
        }

        //Your toolbar is now an action bar and you can use it like you always do, for example:

        lv = (ExpandableHeightListView)findViewById(R.id.list);
        findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "showScanDevicesDialog");
                if (mApp.getPreferredTransport() == Constants.TRANSPORT_GATT) {
                    View addDeviceView = View.inflate(ActivityGroup.this, R.layout.pop_up_add_device, null);
                    showScanDevicesDialog();
                } else {
                    show("Feature not supported in away mode", Toast.LENGTH_SHORT);
                }
            }
        });

        // Methods for previously defined additional features
        getSensorValue =(Button) findViewById(R.id.onbtn);
        showSensorValue =(Button) findViewById(R.id.redbtn);




/*
        config.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                isConfigComplete = false;
//                if(newNode!=null){
//                    Log.d(TAG,"calling configure from UI");
//                    configure(newNode, groupName);
//                }

            }
        });
        */

/*
        brightness = (SeekBar)findViewById(R.id.seekBar6);
        brightness.setMax(255);
        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onProgressChanged" + seekBar.getProgress());
                byte val =(byte) ( seekBar.getProgress()&0xFF);
                serviceReference.OnGroupBrightinessChange(groupId, val, 0);
                adapterGrplist.notifyDataSetChanged();
            }
        });
*/
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.GATT_PROXY_CONNECTED);
        filter.addAction(Constants.GATT_PROXY_DISCONNECTED);
        filter.addAction(Constants.GATT_PROXY_CONNECTED_FOR_CONFIG);
        ActivityGroup.this.registerReceiver(mReceiver, filter);
        //Onclick method for acquiring all sensor data at once
        getSensorValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast for stored value in excel
                Toast.makeText(ActivityGroup.this, "Stored Sensor Values to Excel", Toast.LENGTH_SHORT).show();
                //Arraylist for storing components list
                ArrayList<String> components = new ArrayList<String>(Arrays.asList(serviceReference.getMesh().getGroupComponents(groupName)));

                for(int i=0; i< components.size(); i++) {
                    //Acquiring component name
                    Log.d(TAG,"I+"+i);
                    Log.d(TAG,"Component name"+components.get(i));
                    int[] propValues = serviceReference.getMesh().sensorPropertyListGet(components.get(i));
                    //Acquiring property value for type of sensor

                    if (propValues != null) {
                        Log.d(TAG,"Property Value"+propValues[0]);
                        propertySelected = propValues[0];
                    }
                    //Acquiring individual data value
                    serviceReference.getMesh().sensorGet(components.get(i), propertySelected);




                }

            }
        });
        //Onclick listeners for popdisplay of data representation
        showSensorValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Accessing previously stored data for excel file
                /*
                    try{
                        File folder = new File(Environment.getExternalStorageDirectory()+"/Result/");
                        File file = new File(folder + "/outputFile.xlsx");
                        if (!folder.exists()) {
                            folder.mkdir();
                        }
                    FileInputStream fIP = null;
                    try {
                        fIP = new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    //Get the workbook instance for XLSX file
                    XSSFWorkbook workbook = null;
                    try {
                        workbook = new XSSFWorkbook(fIP);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    XSSFSheet sheet=workbook.getSheet("Sheet Name");
                    Iterator<Row> rowIter = sheet.rowIterator();
                    //Loop for getting data from individual rows and columns for representation
                    int rowno =0;
                    while (rowIter.hasNext()) {
                        Log.e(TAG, " row no "+ rowno );
                        XSSFRow myRow = (XSSFRow) rowIter.next();
                        if(rowno !=0) {
                            Iterator<Cell> cellIter = myRow.cellIterator();
                            int colno =0;
                            String sno="", date="", det="";
                            while (cellIter.hasNext()) {
                                XSSFCell myCell = (XSSFCell) cellIter.next();
                                if (colno==0){
                                    sno = myCell.toString();
                                }else if (colno==1){
                                    date = myCell.toString();
                                }else if (colno==2){
                                    det = myCell.toString();
                                }
                                colno++;
                                Log.e(TAG, " Index :" + myCell.getColumnIndex() + " -- " + myCell.toString());
                            }
                        }
                        rowno++;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "error "+ e.toString());
                } */
                    Toast.makeText(ActivityGroup.this ,"Reached here",Toast.LENGTH_LONG).show();
                Intent intent = new Intent(ActivityGroup.this, PopUp.class);
                startActivity(intent);
                Toast.makeText(ActivityGroup.this ,"Reached here1",Toast.LENGTH_LONG).show();

                /*
                // inflate the layout of the popup window
                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.popresult,null);

                // create the popup window
                int width = LinearLayout.LayoutParams.WRAP_CONTENT;
                int height = LinearLayout.LayoutParams.WRAP_CONTENT;
                boolean focusable = true; // lets taps outside the popup also dismiss it
                final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

                // show the popup window
                // which view you pass in doesn't matter, it is only used for the window tolken
                popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);

                // dismiss the popup window when touched
                popupView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        popupWindow.dismiss();
                        return true;
                    }
                }); */
                }

        });

    }

    public void show(final String text,final int duration) {
        runOnUiThread(new Runnable() {

            public void run() {
                if (mToast == null || !mToast.getView().isShown()) {
                    if (mToast != null) {
                        mToast.cancel();
                    }
                }
               // if (mToast != null) mToast.cancel();
                mToast = Toast.makeText(getApplicationContext(), text, duration);
                mToast.show();
            }
        });

    }

    void popUpAddDevice(final View addDeviceView) {
        final ListView listView = (ListView)addDeviceView.findViewById(R.id.listView6);

        AlertDialog.Builder builder = new AlertDialog.Builder(ActivityGroup.this, R.style.AlertDialogCustom);
        builder.setView(addDeviceView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "User clicked device : " + id);
                String light = components.get(position);
                //LightingService.identify(light);
            }
        });
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                Log.d(TAG, "User clicked OK button");
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d(TAG, "User cancelled the dialog");
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
        alert.getWindow().setLayout(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
    }

    void popUpLight(View lightView) {
        HSVColorPickerDialog cpd = new HSVColorPickerDialog( ActivityGroup.this, 0xFF4488CC, new HSVColorPickerDialog.HSVDailogChangeListener() {
            @Override
            public void onColorSelected(Integer color) {
                Log.d(TAG, "onColorSelected");
            }

            @Override
            public void onColorChanged(Integer color) {
                Log.d(TAG, "onColorChanged");
                mColor = color;
                setGroupColor(mColor);
            }

            @Override
            public void onStopColorChanged() {
                Log.d(TAG, "onStopColorChanged");
                serviceReference.setHSLStopTracking();
            }
            @Override
            public void onStartColorChanged() {
                Log.d(TAG, "onStartColorChanged");
                serviceReference.setHSLStartTracking();
            }
        });

        cpd.setTitle( "Pick a color" );
        cpd.show();
    }

    /*
        void popUpNewGroup(View groupView) {

            //groups = serviceReference.getTopLevelGroups(serviceReference.getCurrentGroup());
            components = serviceReference.getTopLevelLights(groupId);
           // final SelectListAdapter adapter = new SelectListAdapter(ActivityGroup.this,components,groups);

            ListView listview =  (ListView)groupView.findViewById(R.id.listView2);
            listview.setAdapter(adapter);
            AlertDialog.Builder builder = new AlertDialog.Builder(ActivityGroup.this, R.style.AlertDialogCustom);
            builder.setView(groupView);

            ImageButton editButton = (ImageButton) groupView.findViewById(R.id.imageButton2);
            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    boolean result = LightingService.addSubGroup(serviceReference.getCurrentGroup(), "NewGroup");
                    if (result)
                        Log.d(TAG, "added group successfully" + name);
                    else
                        Log.d(TAG, "added unsuccessful" + name);

                    Log.d(TAG, "User clicked OK button" + serviceReference.getCurrentGroup());
                    HashMap<Integer, Boolean> chkbox = adapter.getCheckboxlist();
                    if(groups != null)
                        for (int i = 0; i < groups.size(); i++) {
                            if (chkbox.get(i) == true) {
                                String name = groups.get(i).getName();
                            }
                        }
                    if(components != null)
                        for (int i = 0; i < components.size(); i++) {
                            if(groups != null) {
                                if (chkbox.get(groups.size() + i) == true) {
                                    String name = components.get(i).getName();
                                    result = serviceReference.addLight(serviceReference.getCurrentGroup(), components.get(i), type);
                                    if (result)
                                        Log.d(TAG, "added light successfully" + name);
                                    else
                                        Log.d(TAG, "added unsuccessful" + name);
                                }
                            } else {
                                if (chkbox.get(i) == true) {
                                    String name = components.get(i).getName();
                                    result = serviceReference.addLight(serviceReference.getCurrentGroup(), components.get(i), type);
                                    if (result)
                                        Log.d(TAG, "added light successfully" + name);
                                    else
                                        Log.d(TAG, "added unsuccessful" + name);
                                }
                            }
                        }
                    updateDisplay();
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Log.d(TAG, "User cancelled the dialog");
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
            alert.getWindow().setLayout(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);

        }

    */
    void popUpEdit(View editView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ActivityGroup.this, R.style.AlertDialogCustom);
        final EditText text = (EditText) editView.findViewById(R.id.editText);
        builder.setView(editView);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d(TAG, "User clicked OK button");
                getSupportActionBar().setTitle(text.getText());
                if (!isNameAlreadyUsed(text.getText().toString())) {
                    collapsingToolbarLayout.setTitle(text.getText());
                    serviceReference.getMesh().rename(groupName, text.getText().toString());
                } else {
                    show("RoomName is already in use",
                            Toast.LENGTH_SHORT);
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d(TAG, "User cancelled the dialog");
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
        alert.getWindow().setLayout(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);

    }

    /*
    void popUpEditNewSubgroupName(View editView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ActivityGroup.this, R.style.AlertDialogCustom);
        final EditText text = (EditText) editView.findViewById(R.id.editText);
        builder.setView(editView);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d(TAG, "User clicked OK button");
                getSupportActionBar().setTitle(text.getText());
                LightingService.setGroupName(groupId, text.getText().toString());
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d(TAG, "User cancelled the dialog");
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
        alert.getWindow().setLayout(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
    }
    */
    void popUpDelete(View deleteView) {
/*
        //groups = serviceReference.getTopLevelGroups(serviceReference.getCurrentGroup());
        components = serviceReference.getTopLevelLights(serviceReference.getCurrentGroup());
        final SelectListAdapter adapter = new SelectListAdapter(deleteView.getContext(),components,null);
        ListView listView = (ListView) deleteView.findViewById(R.id.listView10);
        listView.setAdapter(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(ActivityGroup.this, R.style.AlertDialogCustom);
        builder.setView(deleteView);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                HashMap<Integer, Boolean> chkList = adapter.getCheckboxlist();

//                for (int i = 0; i < groups.size(); i++) {
//                    if (chkList.get(i) == true) {
//                        String name = groups.get(i).getName();
//                        Log.d(TAG, "selected group =" + name);
//                        boolean result = LightingService.deleteGroup(groups.get(i));
//                        if (result)
//                            Log.d(TAG, "Deleted group successfully" + name);
//                        else
//                            Log.d(TAG, "Delete unsuccessful" + name);
//                    }
//                }

                for (int i = 0; i < components.size(); i++) {
                    if (chkList.get(i) == true) {
                        String name = components.get(i).getName();
                        Log.d(TAG, "selected light =" + name);
                        boolean result = serviceReference.getMesh().resetDevice(components.get(i));
                        if (result) {
                            Log.d(TAG, "Deleted components successfully" + name);
                            Toast.makeText(ActivityGroup.this, "Deleted light successfully ", Toast.LENGTH_SHORT);
                        } else {
                            Log.d(TAG, "Deleted components unsuccessful" + name);
                            Toast.makeText(ActivityGroup.this, "Deleted light unsuccessful ", Toast.LENGTH_SHORT);
                        }
                    }
                }
                //groups = serviceReference.getTopLevelGroups(serviceReference.getCurrentGroup());
                components = serviceReference.getTopLevelLights(serviceReference.getCurrentGroup());
//                if(groups!= null)
//                    Log.d(TAG,"groups = "+groups);

                if(components!=null)
                    Log.d(TAG, "components = " + components);
                adapterGrplist = new GrpDeviceListAdapter(serviceReference, ActivityGroup.this, components, null, "group", groupId);

                if(adapterGrplist!=null)
                    adapterGrplist.notifyDataSetChanged();
                lv.setAdapter(adapterGrplist);
                Log.d(TAG, "User clicked OK button");
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int idx) {
                Log.d(TAG, "User cancelled the dialog");
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
        alert.getWindow().setLayout(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
*/
    serviceReference.getMesh().deleteGroup(groupName);
    }

    void setGroupColor(int color) {
        float hsl[] = new float[3];
        ColorUtils.colorToHSL(color,hsl);
        int hue = (int)hsl[0];
        int saturation = (int)(hsl[1]*100) ;
        int lightness = (int)(hsl[2]*100);

        Log.d(TAG, "Hue : "+hue);
        Log.d(TAG, "Saturation : "+saturation);
        Log.d(TAG, "Lightness : "+lightness);
        serviceReference.onHslValueChange(groupName, hue, saturation, lightness);

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        try {
            return super.dispatchTouchEvent(ev);
        } catch (Exception e) {
            return false;
        }
    }
    @Override
    public void onBackPressed(){
        FragmentManager fm = getFragmentManager();
        LightingService.popStack();
        if (fm.getBackStackEntryCount() > 0) {
            Log.i(TAG, "popping backstack");
            fm.popBackStack();
        } else {
            Log.i(TAG, "nothing on backstack, calling super");
            super.onBackPressed();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int idx = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (item.getItemId()) {
            case android.R.id.home:
                Log.d(TAG, "home" + item.getItemId());
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*for image*/
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            Log.d(TAG, "onActivityResult: request code = " + requestCode);
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == ActivityGroup.this.RESULT_OK
                    && null != data) {
                Uri selectedImage = data.getData();
                setImage(selectedImage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setImage(Uri selectedImage) {

        try {
            final InputStream imageStream = ActivityGroup.this.getContentResolver().openInputStream(selectedImage);
            final Bitmap selectedImage2 = BitmapFactory.decodeStream(imageStream);
            Drawable drawable = new BitmapDrawable(ActivityGroup.this.getResources(), selectedImage2);
            imageView.setBackground(drawable);
            image = drawable;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "dint find image2");
            e.printStackTrace();
        }
    }


    @Override
    public void onMeshServiceStatusChangeCb(int status) {

    }

    @Override
    public void onDeviceFound(final UUID uuid, final String name) {
        Log.d(TAG, "onDeviceFound device:" + uuid.toString());

        //TODO: unique entries ??
        ActivityGroup.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(peerDevices!=null) {
                    for (int i = 0; i < peerDevices.getCount(); i++) {
                        if (peerDevices.getItem(i).mUUID.equals(uuid)) {
                            //Discard the same device seen again
                            return;
                        }
                    }

                    peerDevices.add(new MeshBluetoothDevice(uuid, name));
                    peerDevices.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onProvisionComplete(final UUID device, final byte status){
        Log.d(TAG, "onProvisionComplete remote node: status"+status);
        newNode = String.valueOf(status);

        //progress.setMessage("Provision complete for selected node");
        if(status == 5) {
            components = new ArrayList<String>(Arrays.asList(serviceReference.getMesh().getGroupComponents(groupName)));
            componentType.clear();
            for(int i =0; i<components.size(); i++)
            {
                componentType.add(serviceReference.getMesh().getComponentType(components.get(i)));
            }
            Log.d(TAG,"Component list"+components);
            show("Provision Complete", Toast.LENGTH_SHORT);
            mLightAddedStatus = LIGHT_ADDED;
            ActivityGroup.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (ActivityGroup.this.components.size() != 0) {
                        adapterGrplist = new GrpDeviceListAdapter(serviceReference,
                                ActivityGroup.this,
                                ActivityGroup.this.components,
                                ActivityGroup.this.componentType,
                                null,
                                "group",
                                groupName);
                        if (adapterGrplist != null)
                            adapterGrplist.notifyDataSetChanged();
                        lv.setAdapter(adapterGrplist);

                        Log.d(TAG, "onProvisionComplete adding light groupId:" + groupName + " UUID= " + status + " type= " + type);
                    }
                }
            });
        }

    }

    @Override
    public void onHslStateChanged(final String deviceName, final int lightness, int hue, int saturation) {
     //   show(" Received HSL :" + lightness + ", from :" + (deviceName), Toast.LENGTH_SHORT);
    }

    @Override
    public void onOnOffStateChanged(final String deviceName, final byte onOff) {
        show(" Received Generic on/off :" + onOff + ", from :" + (deviceName), Toast.LENGTH_SHORT);
    }

    @Override
    public void onLevelStateChanged(final String deviceName, final short level) {
        show(" Received level :" + level + ", from :" + (deviceName), Toast.LENGTH_SHORT);
    }

    @Override
    public void onNetworkConnectionStatusChanged(byte transport, final byte status) {
        Log.d(TAG, "recieved onNetworkConnectionStatusChanged , status = " + status);
        String text = null;
        if(status == IMeshControllerCallback.NETWORK_CONNECTION_STATE_CONNECTED)
        {
            text = "Connected to network";
        }
        if(status == IMeshControllerCallback.NETWORK_CONNECTION_STATE_DISCONNECTED)
            text = "Disconnected from network";

        if(text != null){
            show(text,Toast.LENGTH_SHORT);
        }
        runOnUiThread(new Runnable() {

            public void run() {
                if(status ==  IMeshControllerCallback.NETWORK_CONNECTION_STATE_CONNECTED) {
                    if(mApp.getPreferredTransport()==Constants.TRANSPORT_GATEWAY)
                        proxyConnView.setBackgroundColor(getResources().getColor(R.color.blue));
                    else
                        proxyConnView.setBackgroundColor(getResources().getColor(R.color.green));
                } else if(status == IMeshControllerCallback.NETWORK_CONNECTION_STATE_DISCONNECTED) {
                    proxyConnView.setBackgroundColor(getResources().getColor(R.color.red));
                }
            }
        });
    }

    @Override
    public void onCtlStateChanged(String deviceName, int presentLightness, short presentTemperature, final int targetLightness, short targetTemperature, int remainingTime) {
        show(" Received CTL : "+targetLightness, Toast.LENGTH_SHORT);
    }

    @Override
    public void onNodeConnStateChanged(final byte status, final String componentName) {
        Log.d(TAG,"onNodeConnStateChanged in Group UI");
/*
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
            switch (status) {
            case IMeshControllerCallback.MESH_CLIENT_NODE_WARNING_UNREACHABLE:
                show("Node" +componentName+" failed to connect ", Toast.LENGTH_SHORT);
                break;

            case IMeshControllerCallback.MESH_CLIENT_NODE_ERROR_UNREACHABLE:
                show("!!! Action Required Node " +componentName+" unreachable", Toast.LENGTH_SHORT);
                break;
        }
            }
        }, 1000);
*/
    }

    @Override
    public void onOTAUpgradeStatus(byte status, int percentComplete) {

    }

    @Override
    public void onNetworkOpenedCallback(byte status) {

    }

    @Override
    public void onComponentInfoStatus(byte status, String componentName, String componentInfo) {

    }

    @Override
    public void onDfuStatus(byte status, int currBlkNo, int totalBlks) {

    }

    @Override
    public void onSensorStatusCb(String componentName, int propertyId, byte[] data) {

    }

    @Override
    public void onVendorStatusCb(short src, short companyId, short modelId, byte opcode, byte[] data, short dataLen) {

    }
    @Override
    public void onLightnessStateChanged(String deviceName, int target, int present, int remainingTime) {

    }

    @Override
    public void onLightLcModeStatus(String componentName, int mode) {

    }

    @Override
    public void onLightLcOccupancyModeStatus(String componentName, int mode) {

    }

    @Override
    public void onLightLcPropertyStatus(String componentName, int propertyId, int value) {

    }

    private void startScan() {
        Log.d(TAG, "startScan");
        serviceReference.getMesh().scanMeshDevices(true, null);
    }

    private void stopScan() {
        Log.d(TAG, "stopScan");
        serviceReference.getMesh().scanMeshDevices(false, null);
    }

    /*TODO check the implementation*/
    private void showScanDevicesDialog() {
        if(!isLocationServiceEnabled()) {
            Log.d(TAG, "isLocationServiceEnabled : location is false");
            show("Please turn on the location!!!", Toast.LENGTH_SHORT);
            //return;
        }

        LayoutInflater inflater = ActivityGroup.this.getLayoutInflater();
        View scanDevView = inflater.inflate(R.layout.pop_up_add_device, null);

        final ListView lvPeerDevices = (ListView) scanDevView.findViewById(R.id.listView6);
        final EditText deviceName = (EditText) scanDevView.findViewById(R.id.deviceName);
        List<MeshBluetoothDevice> listDevices = new ArrayList<MeshBluetoothDevice>();

        peerDevices = new ArrayAdapter<MeshBluetoothDevice>(ActivityGroup.this, R.layout.node_list_item, listDevices);
        lvPeerDevices.setAdapter(peerDevices);
        lvPeerDevices.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

        if(lvPeerDevices.getCount() > 0)
            lvPeerDevices.setItemChecked(0, true);

        AlertDialog.Builder builder = new AlertDialog.Builder(ActivityGroup.this, AlertDialog.THEME_HOLO_LIGHT);
        builder.setView(scanDevView);
        //TODO check if network is already connected
      //  int res = serviceReference.getMesh().disconnectNetwork((byte)1);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        startScan();
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d(TAG, "User clicked OK button");
                int itemPos = lvPeerDevices.getCheckedItemPosition();
                Log.d(TAG, "showScanDevicesDialog itemPos: " + itemPos);
                if (itemPos != -1) {
                    mPeerDevice = (MeshBluetoothDevice) lvPeerDevices.getAdapter().getItem(itemPos);
                    Log.d(TAG, "showScanDevicesDialog for node " + " BluetoothDevice:" + mPeerDevice);

                    if(mPeerDevice != null) {
                        Log.d(TAG, "provisioning the mPeerDevice = " + mPeerDevice);
                        mDeviceName = deviceName.getText().toString();
//                            isStoppedScan = true;
                        startProvision = true;

//                            if(!isStoppedScan)
                          isStoppedScan = true;
//                        stopScan();
//                        isStoppedScan = true;

                        //   mSpinTimer.postDelayed(myspinRunnable, PROVISION_SPINNER_TIMEOUT);
                        // startSpin();
                    }
                }

                dialog.dismiss();
            }
        });


        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d(TAG, "User cancelled the dialog");
                isStoppedScan = true;
                stopScan();
            }
        });

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d(TAG, "######## User dismissed the dialog ########");
                if(!isStoppedScan)
                    stopScan();
                isStoppedScan = false;

                if(startProvision) {
                    stopScan();

                    serviceReference.getMesh().setDeviceConfig(
                            null,
                            Constants.DEFAULT_IS_GATT_PROXY,
                            Constants.DEFAULT_IS_FRIEND,
                            Constants.DEFAULT_IS_RELAY,
                            Constants.DEFAULT_SEND_NET_BEACON,
                            Constants.DEFAULT_RELAY_XMIT_COUNT,
                            Constants.DEFAULT_RELAY_XMIT_INTERVAL,
                            Constants.DEFAULT_TTL,
                            Constants.DEFAULT_NET_XMIT_COUNT,
                            Constants.DEFAULT_NET_XMIT_INTERVAL
                    );

                    serviceReference.getMesh().setPublicationConfig(
                            Constants.DEFAULT_PUBLISH_CREDENTIAL_FLAG,
                            Constants.DEFAULT_RETRANSMIT_COUNT,
                            Constants.DEFAULT_RETRANSMIT_INTERVAL,
                            Constants.DEFAULT_PUBLISH_TTL
                    );
                    Log.d(TAG,"calling provision");
                    serviceReference.getMesh().provision(mDeviceName, groupName, mPeerDevice.mUUID, (byte)10);
                    startProvision = false;
                }

            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }
//    Runnable myspinRunnable = new Runnable() {
//        @Override
//        public void run() {
//            stopSpin();
//        }
//    };

    private ServiceConnection mConnection= new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "bound service connected");
            LightingService.MyBinder binder = (LightingService.MyBinder) service;
            serviceReference = binder.getService();
            serviceReference.setCurrentGroup(groupName);
            serviceReference.registerCb(ActivityGroup.this);
            Log.d(TAG, "Required on service");


            updateDisplay();


            if (serviceReference.isConnectedToNetwork()) {
                if (mApp.getPreferredTransport() == Constants.TRANSPORT_GATEWAY)
                    proxyConnView.setBackgroundColor(getResources().getColor(R.color.blue));
                else
                    proxyConnView.setBackgroundColor(getResources().getColor(R.color.green));
            } else {
                proxyConnView.setBackgroundColor(getResources().getColor(R.color.red));
            }
            //int componentType = serviceReference.getMesh().getComponentType(groupName);


        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "bound service disconnected");
            serviceReference = null;
        }
    };

    private void updateDisplay() {
        Log.d(TAG,"Update display");
        components = new ArrayList<String>(Arrays.asList(serviceReference.getMesh().getGroupComponents(groupName)));
        componentType = new ArrayList<Integer>();
        if(components != null && components.size()!=0) {
            for(int i = 0; i< components.size(); i++) {
                Log.d(TAG, "Receiving light" + components.get(i));
                componentType.add(serviceReference.getMesh().getComponentType(components.get(i)));

            }



        }

        ActivityGroup.this.runOnUiThread(new Runnable() {

            public void run() {
                adapter = new GrpDeviceListAdapter(serviceReference, ActivityGroup.this, components,componentType, null, "group", groupName);
                lv.setAdapter(adapter);
                lv.setExpanded(true);
            }
        });
    }

    private boolean isNameAlreadyUsed(String name) {
        ArrayList<String> rooms = (ArrayList<String>) serviceReference.getallRooms();
        Boolean found = false;

        for(int i =0 ;i <rooms.size(); i++) {
            Log.d(TAG, "room name = " + rooms.get(i) + "current room name=" + name);
            if(rooms.get(i).equals(name)) {
                Log.d(TAG, "room name found");
                found = true;
            }
        }
        return found;
    }

//    void startSpin() {
//
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                progress = new ProgressDialog(ActivityGroup.this);
//                progress.setTitle("Provision and Configuration");
//                progress.setMessage("provision in Progress...");
//                progress.setCancelable(false);
//                progress.show();
//
//            }
//        });
//
//    }

    //    void stopSpin() {
//        Log.d(TAG,"Provision failure - Stopping spin");
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                progress.setMessage("Provision failure");
//                progress.dismiss();
//                Toast.makeText(ActivityGroup.this, "Provision failure", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//}


    void popUpTransTime(View editView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ActivityGroup.this, R.style.AlertDialogCustom);
        final EditText text = (EditText) editView.findViewById(R.id.editText);
        builder.setView(editView);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d(TAG, "User clicked OK button");
                //transtime.setText(text.getText().toString());
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d(TAG, "User cancelled the dialog");
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
        alert.getWindow().setLayout(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Received intent: " + action);

            if(action.equals(Constants.GATT_PROXY_CONNECTED)){
                Log.d(TAG, Constants.GATT_PROXY_CONNECTED);
                proxyConnView.setBackgroundColor(getResources().getColor(R.color.green));

                if(ActivityGroup.this!=null) {
                    //serviceReference.isConnectedToNetwork = true;
          //          ActivityGroup.this.runOnUiThread(new Runnable() {
                  //      public void run() {

//                            if(mLightAddedStatus == LIGHT_ADDED) {
//                               progress.setMessage("Configuring...");
//                            }

            //            }
                //    });

                    Log.d(TAG, "Configure from config button");
//                    if(!isConfigComplete)
//                        configure(newNode, groupName);
//                    try {
//                        Thread.sleep(3000);
//                        Log.d(TAG, "Configure from config button");
//                        if(!isConfigComplete)
//                            configure(newNode, groupId);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                }
            } else if(action.equals(Constants.GATT_PROXY_DISCONNECTED)){
                proxyConnView.setBackgroundColor(getResources().getColor(R.color.red));
            } else if(action.equals(Constants.GATT_PROXY_CONNECTED_FOR_CONFIG)) {
                proxyConnView.setBackgroundColor(getResources().getColor(R.color.green));
            }
        }
    };

    public boolean isLocationServiceEnabled(){
        LocationManager locationManager = null;
        boolean gps_enabled= false,network_enabled = false;

        if(locationManager ==null)
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try{
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch(Exception ex){
            //do nothing...
        }

        try{
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }catch(Exception ex){
            //do nothing...
        }

        return gps_enabled || network_enabled;

    }
    public void sentValue(double a, String b){
        Log.d(TAG,"A:"+a);
        Log.d(TAG,"B:"+b);
    }
    //Method for creating excel sheet and adding the header for each column
    private static void createExcel() throws IOException {
        String storageState = Environment.getExternalStorageState();
        if (storageState.equals(Environment.MEDIA_MOUNTED)) {
            //Create Blank workbook
            XSSFWorkbook workbook1 = new XSSFWorkbook();
            XSSFSheet spreadsheet = workbook1.createSheet("Sheet Name");
            XSSFRow row = spreadsheet.createRow(0);
            XSSFCell cell = row.createCell(0);
            cell.setCellValue("Time");
            XSSFCell cell1 = row.createCell(1);
            cell1.setCellValue("Id");
            XSSFCell cell2 = row.createCell(2);
            cell2.setCellValue("Value");
            //Create file system using specific name
            File folder = new File(Environment.getExternalStorageDirectory()+"/Result/");
            File file = new File(folder + "/outputFile.xlsx");
            if (!folder.exists()) {
                folder.mkdir();
            }

            FileOutputStream fos = new FileOutputStream(file);
            //write operation workbook using file out object
            workbook1.write(fos);
            fos.close();
            System.out.println("createworkbook.xlsx written successfully");
            Log.d(TAG, "Create Success");
        }
        // check if available and not read only
        /*String storageState = Environment.getExternalStorageState();
        if (storageState.equals(Environment.MEDIA_MOUNTED)) {
            File file = new File(Environment.getExternalStorageDirectory(), "outputFile.xlsx");
            FileOutputStream fos = new FileOutputStream(file);
            String text = "Hello, world!";
            Log.d(TAG,text);
            fos.write(text.getBytes());
            fos.close();
        }*/
    }
}
