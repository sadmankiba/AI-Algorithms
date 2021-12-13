#include <bits/stdc++.h>
#include <boost/graph/adjacency_list.hpp>
#include <boost/graph/graphviz.hpp>
#include <boost/graph/properties.hpp>
#include <boost/property_map/property_map.hpp>
#include <boost/graph/named_function_params.hpp>
#include <boost/graph/graph_traits.hpp>
#include <boost/graph/sequential_vertex_coloring.hpp>

#define NUM_COURSES 261

using namespace std;
using namespace boost;

typedef boost::adjacency_list<boost::setS, boost::vecS,
                                    boost::undirectedS,
                                    boost::no_property,
                                    boost::property<boost::edge_weight_t, double>
                                > MyGraphType;
typedef boost::iterator_property_map<int*, property_map<MyGraphType, vertex_index_t>::const_type> Colormap;

template <class C, class T> 
void print_container(C v) {
    for (T elem : v) {
        cout << elem << " ";
    }
    cout << "\n";
}

template <class K, class V>
void print_map(map<K, V> m, string key_name, string prop_name) {
    // referenced(&elem) can be used to alter properties
    for (auto elem : m) {
        cout << key_name << " " << elem.first << " " << prop_name << " " << elem.second << endl;
    }
}

void print_colormap(Colormap colormap, MyGraphType G) {
    auto vpair = vertices(G);
    for(auto iter=vpair.first; iter!=vpair.second; iter++) {
        cout << "vertex " << *iter << " color " << colormap[*iter] << endl;
    }
}

// vertex equals to courses
// edge weight equals to # of common students. 
int calculate_penalty(Colormap color, MyGraphType G) {
    boost::property_map<MyGraphType, boost::edge_weight_t>::type weightmap =
            get(boost::edge_weight, G);

    int penalty = 0;
    auto epair = edges(G);
    for(auto iter=epair.first; iter!=epair.second; iter++) {
        // std::cout << "edge " << source(*iter, G) << " - " << target(*iter, G) << std::endl;
        int timeslot_dist = abs(color[source(*iter, G)] - color[target(*iter, G)]);
        if (timeslot_dist <= 5) {
            // cout << "<=2" << endl;
            penalty += (weightmap[*iter] * pow(2, 5 - timeslot_dist));
        }  else {
            penalty += 0;
            // cout << "> 2" << endl;
        }
    }
    return penalty;
}

void swap_color (Colormap colormap, MyGraphType G, int color1, int color2) {
    auto vpair = vertices(G);
    for(auto iter=vpair.first; iter!=vpair.second; iter++) {
        if (colormap[*iter] == color1) 
            colormap[*iter] = color2;
        else if (colormap[*iter] == color2)
            colormap[*iter] = color1;
    }
}

map<int, int> generate_map(Colormap colormap, MyGraphType G) {
    map<int, int> a_map;
    auto vpair = vertices(G);
    for(auto iter=vpair.first; iter!=vpair.second; iter++) {
        a_map[*iter] = colormap[*iter];
    }
    return a_map;
}

void write_map_to_colormap(Colormap colormap, map<int, int> a_map) {
    for (auto el : a_map) {
        colormap[el.first] = el.second;
    }
}

void get_optimal_neighbour(Colormap colormap, MyGraphType G, 
                            int num_slots, int timeslot_to_swap) {
    map<int, int> optimal_neighbour_map = generate_map(colormap, G);
    int optimal_penalty = calculate_penalty(colormap, G);
    
    for (int i = 0; i < num_slots; i++) {
        swap_color(colormap, G, timeslot_to_swap, i);
        int temp_penalty = calculate_penalty(colormap, G);
        if (temp_penalty < optimal_penalty) {
            optimal_neighbour_map = generate_map(colormap, G);
        }
        // Resetting the change for next iteration
        swap_color(colormap, G, timeslot_to_swap, i);
    }
    write_map_to_colormap(colormap, optimal_neighbour_map);
}

// state = course -> timeslot map
// neighbour = any state obtained by swapping a pair of timeslots
// assuming colormap is deep copied
void hill_climbling (Colormap colormap, int num_colors, MyGraphType G) {    
    int MAX_SIDEWAYS = 20;
    int num_sideway_moves = 0;
    while(true) {
        int next_slot_to_swap = (rand() / RAND_MAX * num_colors) % num_colors;
        int current_penalty = calculate_penalty(colormap, G);
        
        get_optimal_neighbour(colormap, G, num_colors, next_slot_to_swap);
        // print_colormap(colormap, G);

        int new_penalty = calculate_penalty(colormap, G);
        // cout << "penalty " << new_penalty << endl;
        if (new_penalty == current_penalty)
            num_sideway_moves++;
            if (num_sideway_moves >= MAX_SIDEWAYS)
                break;
    }
}

int main () {
    srand (time(NULL));
    MyGraphType G(NUM_COURSES);

    boost::property_map<MyGraphType, boost::edge_weight_t>::type weightmap =
            get(boost::edge_weight, G);
    
    // 1. read input & 2. create graph
    ifstream in("tre-s-92.stu");
    int c1, c2 = 0;

    string input;

    // Create course-student graph
    while(in) {
        // Read each line 
        getline(in, input); 
        if(!in) 
            break;

        stringstream ss(input);

        int course;
        vector<int> std_courses;
        while (ss >> course) {
            std_courses.push_back(course);
        }
        // print_container<vector<int>, int>(std_courses);


        // Connect all possible pairs of courses for a single student
        for(int i = 0; i < std_courses.size(); i++) {
            for(int j = i + 1; j < std_courses.size(); j++) {
                // cout << std_courses[i] << std_courses[j] << endl;
                auto e = add_edge(std_courses[i] - 1, std_courses[j] - 1, G);
                
                // Save # of common students as edge weight
                if (e.second) 
                    weightmap[e.first] = 1;
                else 
                    weightmap[e.first] = weightmap[e.first] + 1;
            }
        }
    }
    // 2b. verify each course has got correct # of students
    //for each vertex 
    //      sum weights of edges connected to it
    
    auto vpair = vertices(G);
    for(auto iter=vpair.first; iter!=vpair.second; iter++) {
        int num_students = 0;
        auto oepair = out_edges(*iter, G);
        for(auto iter2 = oepair.first; iter2 != oepair.second; iter2++) {
            //std::cout << "edge " << source(*iter2, G) << " - " << target(*iter2, G) << std::endl;
            num_students += weightmap[*iter2];
        }
        // cout << *iter << " " << num_students << endl;
    }

    // auto epair = edges(G);
    // for(auto iter=epair.first; iter!=epair.second; iter++) {
    //     std::cout << "edge " << source(*iter, G) << " - " << target(*iter, G) << std::endl;
    // }
    in.close();

    // 3. colorize
    vector<int> color_vec(num_vertices(G));
    Colormap colormap(&color_vec.front(), get(vertex_index, G));

    int num_colors = sequential_vertex_coloring(G, colormap);
    // print_colormap(colormap, G);
    cout << endl << "# of timeslots " << num_colors << endl ;

    
    
    // map<int, int> schedule = generate_map(colormap, G);
    // print_map(schedule, "course", "color");
    // schedule[4] = 15;
    // write_map_to_colormap(colormap, schedule);
    // for(auto iter=vpair.first; iter!=vpair.second; iter++) {
    //     cout << "vertex " << *iter << " color " << colormap[*iter] << endl;
    // }

    // 4. timeslot allocate by local search
    cout << "initial penalty: " << (calculate_penalty(colormap, G) * 1.0 / NUM_COURSES) << endl;
    // get_optimal_neighbour(colormap, G, num_colors, 1);\
    // print_colormap(colormap, G);
    hill_climbling(colormap, num_colors, G);
    cout << "penalty: " << (calculate_penalty(colormap, G) * 1.0 / NUM_COURSES) << endl;
}