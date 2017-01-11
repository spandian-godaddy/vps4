package com.godaddy.vps4.web;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class PaginatedResultTest {
    
    public class TestObject{
        public int id;
        public TestObject(int id){
            this.id = id;
        }
    }
    
    private List<TestObject> objList;
    
    @Before
    public void setupTest(){
        this.objList = new ArrayList<TestObject>();
        this.objList.add(new TestObject(1));
        this.objList.add(new TestObject(2));
        this.objList.add(new TestObject(3));
    }
    
    @Test
    public void testConstructor(){
        PaginatedResult<TestObject> pr = new PaginatedResult<>(objList, 3, 5, 10, "testUrl");
        assertEquals(3, pr.pagination.limit);
        assertEquals(10, pr.pagination.total);
        assertEquals("testUrl/3/8", pr.pagination.next);
        assertEquals("testUrl/3/2", pr.pagination.prev);
        assertEquals(objList, pr.results);
    }
    
    @Test
    public void testAtBeginning(){
        PaginatedResult<TestObject> pr = new PaginatedResult<>(objList, 3, 0, 10, "testUrl");
        assertEquals("testUrl/3/0", pr.pagination.prev);
        
        pr = new PaginatedResult<>(objList, 3, 1, 10, "testUrl");
        assertEquals("testUrl/3/0", pr.pagination.prev);
        
        pr = new PaginatedResult<>(objList, 3, 3, 10, "testUrl");
        assertEquals("testUrl/3/0", pr.pagination.prev);
        
        pr = new PaginatedResult<>(objList, 3, 4, 10, "testUrl");
        assertEquals("testUrl/3/1", pr.pagination.prev);
    }
    
    @Test
    public void testAtEnd(){
        PaginatedResult<TestObject> pr = new PaginatedResult<>(objList, 3, 6, 10, "testUrl");
        assertEquals("testUrl/3/9", pr.pagination.next);
        
        pr = new PaginatedResult<>(objList, 3, 7, 10, "testUrl");
        assertEquals("testUrl/3/7", pr.pagination.next);
        
        pr = new PaginatedResult<>(objList, 3, 10, 10, "testUrl");
        assertEquals("testUrl/3/10", pr.pagination.next);
    }
    
    

}
