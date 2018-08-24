package com.findclass.ajvm.findclassapp.menuActivities;

import android.content.Intent;
import android.os.Bundle;
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
import com.findclass.ajvm.findclassapp.Adapter.SubjectProfessorAdapter;
import com.findclass.ajvm.findclassapp.Helper.RecyclerItemClickListener;
import com.findclass.ajvm.findclassapp.Model.Professor_Subject;
import com.findclass.ajvm.findclassapp.Model.Subject;
import com.findclass.ajvm.findclassapp.Model.Subject_Professor;
import com.findclass.ajvm.findclassapp.Model.User;
import com.findclass.ajvm.findclassapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SubjectCategoryLevelActivity extends AppCompatActivity {
    private RecyclerView recyclerViewMedio;
    private SubjectProfessorAdapter adapter;
    private ArrayList<Subject_Professor> listProfessors = new ArrayList<>();
    private DatabaseReference professorSubjectRef;
    private DatabaseReference userRef;
    private DatabaseReference subjectRef;
    private ValueEventListener valueEventListenerProfessores;
    private MaterialSearchView searchView;
    private String nameSubject;
    private String nameLevel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_category_level);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerViewMedio = findViewById(R.id.recycleViewListaMedio);
        professorSubjectRef = FirebaseDatabase.getInstance().getReference().child("professorSubjects");
        userRef = FirebaseDatabase.getInstance().getReference().child("users");
        subjectRef = FirebaseDatabase.getInstance().getReference().child("subjects");

        adapter = new SubjectProfessorAdapter(listProfessors, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewMedio.setLayoutManager(layoutManager);
        recyclerViewMedio.setHasFixedSize(true);
        recyclerViewMedio.setAdapter(adapter);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            nameSubject = (String) bundle.getSerializable("subject");
            nameLevel = (String)bundle.getSerializable("level");
        }

        searchView = findViewById(R.id.search_viewProfessor);
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
                    searchProfessor(query.toLowerCase());
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
                    searchProfessor(newText.toLowerCase());
                }

                return true;
            }
        });

        recyclerViewMedio.addOnItemTouchListener(
                new RecyclerItemClickListener(
                        this,
                        recyclerViewMedio,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                Intent intent = new Intent(SubjectCategoryLevelActivity.this,
                                        AvailabilityListAlunoActivity.class);
                                Subject_Professor thisSubjectProfessor = listProfessors.get(position);
                                intent.putExtra("professor_uid",thisSubjectProfessor.getProfessorSubject().getProfessorUid());
                                intent.putExtra("subject_id",thisSubjectProfessor.getSubject().getId());

                                startActivity(intent);
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
    public void onBackPressed() {
        Intent intent = new Intent(getBaseContext(), MenuAlunoActivity.class);
        startActivity(intent);
        finish();
    }


    @Override
    protected void onStart() {
        super.onStart();
        retrieveProfessors();
    }

    @Override
    protected void onStop() {
        super.onStop();
        subjectRef.removeEventListener(valueEventListenerProfessores);
        professorSubjectRef.removeEventListener(valueEventListenerProfessores);
        userRef.removeEventListener(valueEventListenerProfessores);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_aluno, menu);
        MenuItem item = menu.findItem(R.id.menuPesquisa);
        searchView.setMenuItem(item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logoutAluno) {
            logout(this.findViewById(R.id.toolbar));
        }

        return super.onOptionsItemSelected(item);
    }
    public void logout(View view){
        try{
            FirebaseAuth.getInstance().signOut();
        }catch (Exception e){
            String message = "Erro, você já está delogado";
            Toast.makeText(this,message,Toast.LENGTH_LONG).show();
        }
        Toast.makeText(this, "Você foi deslogado!", Toast.LENGTH_LONG).show();
        startActivity(new Intent(SubjectCategoryLevelActivity.this,SignInActivity.class));
        finish();
    }

    public void searchProfessor(String text) {
        List<Subject_Professor> listProfessorSearch = new ArrayList<>();
        for (Subject_Professor professor : listProfessors) {
            String subject = professor.getUser().getName().toLowerCase();
            subject = subject.replace('á', 'a');
            subject = subject.replace('ã', 'a');
            subject = subject.replace('é', 'e');
            subject = subject.replace('ê', 'e');
            subject = subject.replace('ó', 'o');
            subject = subject.replace('õ', 'o');
            subject = subject.replace('ú', 'u');
            subject = subject.replace('í', 'i');

            if (subject.contains(text)) {
                listProfessorSearch.add(professor);

            }
        }
        adapter = new SubjectProfessorAdapter(listProfessorSearch, this);
        recyclerViewMedio.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    public void reloadList() {
        adapter = new SubjectProfessorAdapter(listProfessors, this);
        recyclerViewMedio.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    public void retrieveProfessors (){
        listProfessors.clear();
        valueEventListenerProfessores = professorSubjectRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (final DataSnapshot dados : dataSnapshot.getChildren()) {
                    professorSubjectRef.child(dados.getKey()).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (final DataSnapshot dado : dataSnapshot.getChildren()) {
                                final Subject_Professor sp = new Subject_Professor();
                                final Professor_Subject ps = dado.getValue(Professor_Subject.class);
                                sp.setProfessorSubject(ps);
                                userRef.addValueEventListener(
                                        new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                for (DataSnapshot d : dataSnapshot.getChildren()) {
                                                    User user = d.getValue(User.class);
                                                    if (d.getKey().equals(ps.getProfessorUid())) {
                                                        sp.setUser(user);

                                                        subjectRef.addValueEventListener(
                                                                new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                                                        for (DataSnapshot d : dataSnapshot.getChildren()) {
                                                                            Subject subject = d.getValue(Subject.class);
                                                                            if (d.getKey().equals(ps.getSubjectId()) && subject.getLevel().equals(nameLevel)&& subject.getName().equals(nameSubject)) {
                                                                                sp.setSubject(subject);
                                                                                listProfessors.add(sp);
                                                                                Collections.sort(listProfessors);


                                                                            }
                                                                            adapter.notifyDataSetChanged();
                                                                        }
                                                                        adapter.notifyDataSetChanged();
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
                                        }
                                );
                            }

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            //
                        }
                    });


                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //
            }
        });
    }

}

