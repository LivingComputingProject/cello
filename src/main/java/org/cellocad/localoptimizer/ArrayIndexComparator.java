/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cellocad.localoptimizer;

import java.util.ArrayList;
import java.util.Comparator;


/**
 *
 * @author arashkh
 * 
 * This is a simple sorting comparator used for updating the orders for the second pass of the algorithm
 */
public class ArrayIndexComparator implements Comparator<Integer>
{
    private final ArrayList<Double> list;

    public ArrayIndexComparator(ArrayList<Double> list)
    {
        this.list = list;
    }
    
    public ArrayList<Integer> createIndexArray()
    {
        ArrayList<Integer> indexes = new ArrayList();
        for (int i = 0; i < list.size(); i++)
        {
            indexes.add(i+1); // Autoboxing
        }
        return indexes;
    }

    @Override
    public int compare(Integer index1, Integer index2)
    {
         // Autounbox from Integer to int to use as array indexes
        return list.get(index1-1).compareTo(list.get(index2-1));
    }
}
