package com.findclass.ajvm.findclassapp.menuActivities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.findclass.ajvm.findclassapp.AccountActivities.SignInActivity;
import com.findclass.ajvm.findclassapp.Adapter.AvailabilityListAdapter;
import com.findclass.ajvm.findclassapp.Helper.RecyclerItemClickListener;
import com.findclass.ajvm.findclassapp.Model.Date_Status;
import com.findclass.ajvm.findclassapp.Model.Date_Time;
import com.findclass.ajvm.findclassapp.Model.Schedule;
import com.findclass.ajvm.findclassapp.Model.Time;
import com.findclass.ajvm.findclassapp.Model.Time_Date;
import com.findclass.ajvm.findclassapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


public class AvailabilityListAlunoActivity extends AppCompatActivity {
    private RecyclerView recyclerViewAvailability;
    private AvailabilityListAdapter adapter;
    private ArrayList<Time_Date> listTimeDates = new ArrayList<>();
    private DatabaseReference dateTimeRef;
    private DatabaseReference professorRef;
    private String professorUid;
    private DatabaseReference subjectRef;
    private DatabaseReference scheduleRef;
    private DatabaseReference timeRef;
    private DatabaseReference dateRef;
    private String subjectId;
    private ValueEventListener valueEventListenerProfessores;
    private MaterialSearchView searchView;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private static PayPalConfiguration config;
    private Schedule paypalSchedule;
    private Time_Date paypalTimeDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_availability_list_aluno);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle data = getIntent().getExtras();
        subjectId = (String) data.getSerializable("subject_id");
        professorUid = (String) data.getSerializable("professor_uid");

        config = new PayPalConfiguration()
                // Start with mock environment (ENVIROMENT_NO_NETWORK).
                // When ready, switch to sandbox (ENVIRONMENT_SANDBOX)
                // or live (ENVIRONMENT_PRODUCTION)
                .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
                .clientId("ATl_uNA7Rar8H-JXwMdRWwvVBC8DcFsPq3FspiwcWmqFqOkOOT2o4m-9KipNIhfMp5PhfPyqcL-5pD2a")
                .merchantName("FindClass");

        recyclerViewAvailability = findViewById(R.id.recycleViewAvailabilityList);
        professorRef = FirebaseDatabase.getInstance().getReference().child("users");
        subjectRef = FirebaseDatabase.getInstance().getReference().child("subjects");
        dateTimeRef = FirebaseDatabase.getInstance().getReference().child("availability").child(professorUid).child("dateTimes");
        timeRef = FirebaseDatabase.getInstance().getReference().child("availability").child(professorUid).child("times");
        dateRef = FirebaseDatabase.getInstance().getReference().child("availability").child(professorUid).child("dates");

        scheduleRef = FirebaseDatabase.getInstance().getReference().child("schedule");

        adapter = new AvailabilityListAdapter(listTimeDates, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewAvailability.setLayoutManager(layoutManager);
        recyclerViewAvailability.setHasFixedSize(true);
        recyclerViewAvailability.setAdapter(adapter);


        searchView = findViewById(R.id.search_viewDays);
        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {

            }

            @Override
            public void onSearchViewClosed() {
                reloadList();
            }
        });


        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                query = query.replace('á', 'a');
                query = query.replace('ã', 'a');
                query = query.replace('é', 'e');
                query = query.replace('ê', 'e');
                query = query.replace('ó', 'o');
                query = query.replace('õ', 'o');
                query = query.replace('ú', 'u');
                query = query.replace('í', 'i');

                if (query != null && !query.isEmpty()) {
                    searchDay(query.toLowerCase());
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                newText = newText.replace('á', 'a');
                newText = newText.replace('ã', 'a');
                newText = newText.replace('é', 'e');
                newText = newText.replace('ê', 'e');
                newText = newText.replace('ó', 'o');
                newText = newText.replace('õ', 'o');
                newText = newText.replace('ú', 'u');
                newText = newText.replace('í', 'i');

                if (newText != null && !newText.isEmpty()) {
                    searchDay(newText.toLowerCase());
                }

                return true;
            }
        });

        recyclerViewAvailability.addOnItemTouchListener(
                new RecyclerItemClickListener(
                        this,
                        recyclerViewAvailability,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {

                                final Time_Date thisTimeDate = listTimeDates.get(position);
                                final Schedule schedule = new Schedule();
                                schedule.setProfessor_id(professorUid);
                                schedule.setStudent_id(auth.getCurrentUser().getUid());
                                schedule.setSubject_id(subjectId);
                                schedule.setDatetime_id(thisTimeDate.getDate_time_id());
                                schedule.setRating("0");
                                DatabaseReference schedulePush = scheduleRef
                                        .child(professorUid)
                                        .child(auth.getCurrentUser().getUid())
                                        .push();
                                schedule.setId(schedulePush.getKey());

                                paypalSchedule = schedule;
                                paypalTimeDate = thisTimeDate;

                                // PAYMENT_INTENT_SALE will cause the payment to complete immediately.
                                // Change PAYMENT_INTENT_SALE to
                                //   - PAYMENT_INTENT_AUTHORIZE to only authorize payment and capture funds later.
                                //   - PAYMENT_INTENT_ORDER to create a payment for authorization and capture
                                //     later via calls from your server.

                                PayPalPayment payment = new PayPalPayment(
                                        new BigDecimal(
                                                thisTimeDate.getTime().getPrice().toString()
                                                        .replace("R$","")
                                                        .replace(",",".")
                                        ),
                                        "BRL",
                                        ("FindClass/"+schedule.getId()),
                                        PayPalPayment.PAYMENT_INTENT_SALE
                                );

                                Intent intent = new Intent(getBaseContext(), PaymentActivity.class);

                                // send the same configuration for restart resiliency
                                intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);

                                intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);

                                startActivityForResult(intent, 0);

                                //Intent intent = new Intent(getBaseContext(),MenuAlunoActivity.class);



//
//                                schedulePush.setValue(schedule);
//                                dateTimeRef.child(thisTimeDate.getDate_time_id()).child("status").setValue("sim");
//
//                                startActivity(intent);

                                //Toast.makeText(getBaseContext(),"FON",Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onLongItemClick(View view, int position) {
                                //
                            }

                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                //
                            }
                        }
                )
        );

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logoutAluno) {
            logout(this.findViewById(R.id.toolbar));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        String message = "";
        if (resultCode == Activity.RESULT_OK) {
            PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
            if (confirm != null) {
                try {
                    Log.i("paymentExample", confirm.toJSONObject().toString(4));
                    message = "Pagamento efetuado com sucesso!";

                    Intent intent = new Intent(getBaseContext(),MenuAlunoActivity.class);

                    scheduleRef
                            .child(professorUid)
                            .child(auth.getCurrentUser().getUid())
                            .child(paypalSchedule.getId())
                            .setValue(paypalSchedule);

                    dateTimeRef
                            .child(paypalTimeDate.getDate_time_id())
                            .child("status")
                            .setValue("sim");

                    startActivity(intent);

                    // TODO: send 'confirm' to your server for verification.
                    // see https://developer.paypal.com/webapps/developer/docs/integration/mobile/verify-mobile-payment/
                    // for more details.

                } catch (JSONException e) {
                    Log.e("paymentExample", "an extremely unlikely failure occurred: ", e);
                    message = "Erro...";
                }
            }
        }
        else if (resultCode == Activity.RESULT_CANCELED) {
            Log.i("paymentExample", "The user canceled.");
            message = "Cancelado...";
        }
        else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
            Log.i("paymentExample", "An invalid Payment or PayPalConfiguration was submitted. Please see the docs.");
            message = "Erro na API...";
        }
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getBaseContext(), MenuAlunoActivity.class);
        startActivity(intent);
        finish();
    }


    @Override
    protected void onStart() {
        super.onStart();
        retrieveDateTimes();
    }

    @Override
    protected void onStop() {
        super.onStop();
        subjectRef.removeEventListener(valueEventListenerProfessores);
        dateTimeRef.removeEventListener(valueEventListenerProfessores);
        professorRef.removeEventListener(valueEventListenerProfessores);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_aluno, menu);
        MenuItem item = menu.findItem(R.id.menuPesquisa);
        searchView.setMenuItem(item);
        return true;
    }

    public void searchDay(String text) {
        List<Time_Date> listDaySearch = new ArrayList<>();
        for (Time_Date time_date : listTimeDates) {
            String day = time_date.getTime().getDay().toLowerCase();
            day = day.replace('á', 'a');
            day = day.replace('ã', 'a');
            day = day.replace('é', 'e');
            day = day.replace('ê', 'e');
            day = day.replace('ó', 'o');
            day = day.replace('õ', 'o');
            day = day.replace('ú', 'u');
            day = day.replace('í', 'i');

            if (day.contains(text)) {
                listDaySearch.add(time_date);

            }
        }
        adapter = new AvailabilityListAdapter(listDaySearch, this);
        recyclerViewAvailability.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    public void reloadList() {
        adapter = new AvailabilityListAdapter(listTimeDates, this);
        recyclerViewAvailability.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    public void retrieveDateTimes(){
        listTimeDates.clear();
        valueEventListenerProfessores = dateTimeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (final DataSnapshot dado : dataSnapshot.getChildren()) {
                    final Time_Date td = new Time_Date();
                    final Date_Time dt = dado.getValue(Date_Time.class);
                    td.setDate_time(dt);

                    td.setDate_time_id(dado.getKey());
                    if (dado.child("status").getValue(String.class).equals("não")){
                        timeRef.addValueEventListener(
                                new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        for (DataSnapshot d : dataSnapshot.getChildren()) {
                                            Time time = d.getValue(Time.class);
                                            if (d.getKey().equals(dado.child("time_id").getValue())) {
                                                td.setTime(time);

                                            }adapter.notifyDataSetChanged();
                                        }adapter.notifyDataSetChanged();

                                    }


                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        //
                                    }
                                }
                        );
                        dateRef.addValueEventListener(
                                new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        for (DataSnapshot d : dataSnapshot.getChildren()) {
                                            Date_Status ds = d.getValue(Date_Status.class);
                                            if (d.getKey().equals(dado.child("date_id").getValue())) {
                                                td.setDate_status(ds);
                                                listTimeDates.add(td);
                                                adapter.notifyDataSetChanged();
                                            }adapter.notifyDataSetChanged();
                                        }adapter.notifyDataSetChanged();
                                    }


                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        //
                                    }
                                }
                        );
                    }

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //
            }
        });

    }
    public void logout(View view){
        try{
            FirebaseAuth.getInstance().signOut();
        }catch (Exception e){
            String message = "Erro, você já está delogado";
            Toast.makeText(this,message,Toast.LENGTH_LONG).show();
        }
        Toast.makeText(this, "Saiu!", Toast.LENGTH_LONG).show();
        startActivity(new Intent(AvailabilityListAlunoActivity.this,SignInActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(this, PayPalService.class));
        super.onDestroy();
    }
}

