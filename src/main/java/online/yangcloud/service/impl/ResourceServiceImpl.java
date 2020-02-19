package online.yangcloud.service.impl;

import online.yangcloud.entity.Resource;
import online.yangcloud.mapper.ResourceMapper;
import online.yangcloud.service.ResourceService;
import online.yangcloud.tools.GlobalConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author 小阳Sheep.
 * created in 2019/12/26 9:48
 */

@Service
public class ResourceServiceImpl implements ResourceService {

    private Logger logger = LoggerFactory.getLogger(ResourceServiceImpl.class);

    private final ResourceMapper resourceMapper;

    public ResourceServiceImpl(ResourceMapper resourceMapper) {
        this.resourceMapper = resourceMapper;
    }

    @Override
    public Resource addResource(Resource resource) {
        if (resource.getType().equals(GlobalConstant.FOLDER))
            resource.setChildren(resourceMapper.findMaxChildrenNumber() + 1);
        resource.setCtime(new Date());
        resource.setCount(0);
        Integer sort = resourceMapper.findMaxSortByFather(resource.getFather());
        if (sort == null) resource.setSort(0);
        else resource.setSort(sort + 1);
        int result = resourceMapper.addResource(resource);
        if (result > 0) {
            return resourceMapper.findByLastTime();
        }
        return null;
    }

    @Override
    public int delResource(int id, String type) {
        if (!GlobalConstant.FOLDER.equalsIgnoreCase(type)) {
            return resourceMapper.delResourceById(id);
        }
        delFile(resourceMapper.findResourceById(id));
        return GlobalConstant.SUCCESS;
    }

    private void delFile(Resource resource) {
        if (GlobalConstant.FOLDER.equalsIgnoreCase(resource.getType())) {
            if (!StringUtils.isEmpty(resource.getChildren())) {
                List<Resource> resources = resourceMapper.findAllByFather(resource.getChildren());
                for (Resource item : resources) {
                    delFile(item);
                }
            }
        }
        resourceMapper.delResourceById(resource.getId());
    }

    @Override
    public int updateResource(Resource resource) {
        Integer newFather = resource.getFather();
        resource = resourceMapper.findResourceById(resource.getId());
        if (!newFather.equals(resource.getFather())) {
            int oldFather = resource.getFather();
            logger.info("id {}, newFather {}, oldFather {}", resource.getId(), newFather, oldFather);
            List<Resource> resources = resourceMapper.findAllByFather(oldFather);
            List<Resource> newResources = new ArrayList<>();
            for (int i = resource.getSort() + 1; i < resources.size(); i++) {
                Resource item = resources.get(i);
                item.setSort(i - 1);
                newResources.add(item);
            }
            if (newResources.size() != 0) {
                int result = resourceMapper.updateResources(newResources);
                System.out.println("成功 " + result + " 条");
            }
            resource.setFather(newFather);
            resource.setSort(resourceMapper.findMaxSortByFather(resource.getFather()) + 1);
        }
        return resourceMapper.updateResource(resource);
    }

    @Override
    public Map<String, Object> findAll(Integer fatherId) {
        Map<String, Object> map = new HashMap<>(2);
        if (fatherId == 0) {
            map.put("path", "当前定位：/");
            map.put("resource", resourceMapper.findAllByFather(fatherId));
        } else {
            Resource resource = resourceMapper.findByChildren(fatherId);
            List<String> path = new ArrayList<>();
            path.add(resource.getName());
            path.add("/");
            int father = resource.getFather();
            while (father != 0) {
                Resource newResource = resourceMapper.findByChildren(father);
                path.add(0, "/");
                path.add(0, newResource.getName());
                father = newResource.getFather();
            }
            String resourcesPath = path.toString();
            resourcesPath = resourcesPath.substring(1, resourcesPath.length() - 1).replace(", ", "");
            map.put("path", "当前定位：/" + resourcesPath);
            map.put("resource", resourceMapper.findAllByFather(resource.getChildren()));
        }
        return map;
    }

    @Override
    public Resource findById(int id) {
        return resourceMapper.findResourceById(id);
    }

    @Override
    public Resource findByChildren(int children) {
        return resourceMapper.findByChildren(children);
    }

    @Override
    public List<Map<String, Object>> getTreeStructure() {
        return getTreeChildren(0);
    }

    private List<Map<String, Object>> getTreeChildren(int father) {
        List<Map<String, Object>> resultMap = new ArrayList<>();
        if (father == 0) {
            Map<String, Object> resourcesMap = new HashMap<>();
            resourcesMap.put("id", father);
            resourcesMap.put("title", "/");
            resourcesMap.put("children", new ArrayList<>());
            resultMap.add(resourcesMap);
        }
        List<Resource> resources = resourceMapper.findAllByFather(father);
        for (Resource resource : resources) {
            if (GlobalConstant.FOLDER.equals(resource.getType())) {
                Map<String, Object> resourcesMap = new HashMap<>();
                resourcesMap.put("id", resource.getChildren());
                resourcesMap.put("title", resource.getName());
                if (!StringUtils.isEmpty(resource.getChildren()))
                    resourcesMap.put("children", getTreeChildren(resource.getChildren()));
                resultMap.add(resourcesMap);
            }
        }
        return resultMap;
    }

    @Override
    public int rankResources(int father, int oldIndex, int newIndex) {
        Resource resource;
        int res;
        List<Resource> resources = resourceMapper.findAllByFather(father);
        if (oldIndex > newIndex) {
            resource = resources.get(oldIndex);
            resource.setSort(newIndex);
            res = resourceMapper.updateResource(resource);
            for (int i = newIndex; i < oldIndex; i++) {
                resource = resources.get(i);
                resource.setSort(i + 1);
                int result = resourceMapper.updateResource(resource);
                if (result > 0) res++;
            }
            return res == newIndex - oldIndex + 1 ? GlobalConstant.SUCCESS : GlobalConstant.ERROR;
        }
        resource = resources.get(oldIndex);
        resource.setSort(newIndex);
        res = resourceMapper.updateResource(resource);
        for (int i = oldIndex + 1; i <= newIndex; i++) {
            resource = resources.get(i);
            resource.setSort(i - 1);
            int result = resourceMapper.updateResource(resource);
            if (result > 0) res++;
        }
        return res == newIndex - oldIndex + 1 ? GlobalConstant.SUCCESS : GlobalConstant.ERROR;
    }

    @Override
    public void updateCount(int id) {
        Resource resource = resourceMapper.findResourceById(id);
        resource.setCount(resource.getCount() + 1);
        resourceMapper.updateResource(resource);
    }
}
