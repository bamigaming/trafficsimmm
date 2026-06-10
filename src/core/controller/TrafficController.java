package core.controller;

import core.constants.Direction;
import core.environment.Intersection;
import core.vehicles.*;
import core.strategies.*;

import java.awt.Rectangle;
import java.util.*;
import java.lang.Math;

public class TrafficController {
    private List<Intersection> intersections;
    private List<Vehicle> vehicles;
    private final int width = 1600, height = 900; // Khóa chặt kích thước logic cố định
    private int roadWidth = 240;
    private int roadStartY = 220;
    private int maxCapacity = 40;

    public int WEST_LANE_SLOW = 224, WEST_LANE_FAST = 264, WEST_LANE_EMG = 304;
    public int EAST_LANE_EMG = 344, EAST_LANE_FAST = 384, EAST_LANE_SLOW = 424;

    private int ticks = 0;
    private Map<Integer, Integer> laneChangeCooldown = new HashMap<>();
    private Set<Integer> justYielded = new HashSet<>();

    public TrafficController(List<Intersection> intersections, List<Vehicle> vehicles) {
        this.intersections = intersections;
        this.vehicles = vehicles;
    }

    private boolean canChangeLane(Vehicle v) {
        int last = laneChangeCooldown.getOrDefault(System.identityHashCode(v), -999);
        return (ticks - last) > 15;
    }

    // ======================== SPAWN VEHICLE ========================
    public void spawnVehicle() {
        if (vehicles.size() >= maxCapacity) return;
        Random rand = new Random();
        if (rand.nextBoolean()) {
            spawnHorizontalVehicle();
        } else {
            spawnVerticalVehicle();
        }
    }

    private void spawnHorizontalVehicle() {
        List<Class<? extends Vehicle>> types = Arrays.asList(Car.class, Car.class, Car.class, Motorcycle.class, Bicycle.class, Ambulance.class, FireTruck.class);
        Class<? extends Vehicle> vehicleClass = types.get(new Random().nextInt(types.size()));

        Optional<Intersection> inter5 = intersections.stream().filter(i -> i.type.equals("5way")).findFirst();
        Random rand = new Random();
        if (inter5.isPresent() && rand.nextDouble() < 0.20) {
            Intersection inter = inter5.get();
            int cx5 = inter.x + roadWidth / 2;
            int cy5 = roadStartY + roadWidth / 2;
            double spawnX = width + 60;
            double laneOffset = 25;
            double spawnY = cy5 - (spawnX - cx5) - 85;
            Direction dir = Direction.SOUTHWEST;
            for (Vehicle v : vehicles) {
                if (v.getDirection() == dir && Math.hypot(v.getX() - spawnX, v.getY() - spawnY) < 160) return;
            }
            Vehicle newV = createVehicle(vehicleClass, spawnX, spawnY, dir);
            if (newV != null) {
                newV.setHasTurned(false);
                newV.setDriverStrategy(getStrategyForVehicle(newV));
                vehicles.add(newV);
            }
            return;
        }

        boolean isEast = rand.nextBoolean();
        Direction dir = isEast ? Direction.EAST : Direction.WEST;
        double spawnX = isEast ? -60 : width + 60;
        List<Integer> possibleLanes = new ArrayList<>();
        if (isEast) {
            if (vehicleClass == Ambulance.class || vehicleClass == FireTruck.class) {
                possibleLanes = Arrays.asList(EAST_LANE_FAST, EAST_LANE_EMG);
            } else if (vehicleClass == Car.class) possibleLanes = Arrays.asList(EAST_LANE_SLOW, EAST_LANE_FAST);
            else possibleLanes = Collections.singletonList(EAST_LANE_SLOW);
        } else {
            if (vehicleClass == Ambulance.class || vehicleClass == FireTruck.class) {
                possibleLanes = Arrays.asList(WEST_LANE_FAST, WEST_LANE_EMG);
            } else if (vehicleClass == Car.class) possibleLanes = Arrays.asList(WEST_LANE_SLOW, WEST_LANE_FAST);
            else possibleLanes = Collections.singletonList(WEST_LANE_SLOW);
        }
        double spawnY = possibleLanes.get(rand.nextInt(possibleLanes.size()));
        for (Vehicle v : vehicles) {
            if (v.getDirection() == dir && Math.abs(v.getY() - spawnY) < 1 && Math.abs(v.getX() - spawnX) < 160) return;
        }
        Vehicle newV = createVehicle(vehicleClass, spawnX, spawnY, dir);
        if (newV != null) {
            newV.setDriverStrategy(getStrategyForVehicle(newV));
            vehicles.add(newV);
        }
    }

    private void spawnVerticalVehicle() {
        if (vehicles.size() >= maxCapacity) return;
        List<Intersection> validIntersections = new ArrayList<>(intersections);
        if (validIntersections.isEmpty()) return;
        Intersection inter = validIntersections.get(new Random().nextInt(validIntersections.size()));
        boolean isNorth = new Random().nextBoolean();
        if (!isNorth && inter.type.equals("3way")) return;

        Direction dir = isNorth ? Direction.NORTH : Direction.SOUTH;
        double spawnY = isNorth ? height + 60 : -60;

        List<Class<? extends Vehicle>> types = Arrays.asList(Car.class, Car.class, Car.class, Motorcycle.class, Bicycle.class, Ambulance.class, FireTruck.class);
        Class<? extends Vehicle> vehicleClass = types.get(new Random().nextInt(types.size()));

        List<Integer> possibleLanesX = new ArrayList<>();
        if (dir == Direction.SOUTH) {
            if (vehicleClass == Ambulance.class || vehicleClass == FireTruck.class) {
                possibleLanesX.add(getSouthFastX(inter));
                possibleLanesX.add(getSouthEmgX(inter));
            } else if (vehicleClass == Car.class) {
                possibleLanesX.add(getSouthSlowX(inter));
                possibleLanesX.add(getSouthFastX(inter));
            } else {
                possibleLanesX.add(getSouthSlowX(inter));
            }
        } else { // NORTH
            if (vehicleClass == Ambulance.class || vehicleClass == FireTruck.class) {
                possibleLanesX.add(getNorthFastX(inter));
                possibleLanesX.add(getNorthEmgX(inter));
            } else if (vehicleClass == Car.class) {
                possibleLanesX.add(getNorthSlowX(inter));
                possibleLanesX.add(getNorthFastX(inter));
            } else {
                possibleLanesX.add(getNorthSlowX(inter));
            }
        }
        double spawnX = possibleLanesX.get(new Random().nextInt(possibleLanesX.size()));

        for (Vehicle v : vehicles) {
            if (v.getDirection() == dir && Math.abs(v.getX() - spawnX) < 60 && Math.abs(v.getY() - spawnY) < 100) {
                return;
            }
        }
        Vehicle newV = createVehicle(vehicleClass, spawnX, spawnY, dir);
        if (newV != null) {
            newV.setDriverStrategy(getStrategyForVehicle(newV));
            if (dir == Direction.NORTH && inter.type.equals("3way")) {
                Random rand = new Random();
                String forcedTurn = rand.nextBoolean() ? "LEFT" : "RIGHT";
                newV.setTurnIntent(forcedTurn);
            }
            vehicles.add(newV);
        }
    }

    private Vehicle createVehicle(Class<? extends Vehicle> clazz, double x, double y, Direction dir) {
        try {
            if (clazz == Car.class) return new Car(x, y, dir);
            if (clazz == Motorcycle.class) return new Motorcycle(x, y, dir);
            if (clazz == Bicycle.class) return new Bicycle(x, y, dir);
            if (clazz == Ambulance.class) return new Ambulance(x, y, dir);
            if (clazz == FireTruck.class) return new FireTruck(x, y, dir);
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private DriverStrategy getStrategyForVehicle(Vehicle v) {
        if (v.getName().equals("Ambu") || v.getName().equals("Fire")) return new EmergencyDriver();
        if (v.getName().equals("Motor")) return new AggressiveDriver();
        return new NormalDriver();
    }

    private int getSouthSlowX(Intersection inter) { return inter.x + 4; }
    private int getSouthFastX(Intersection inter) { return inter.x + 44; }
    private int getSouthEmgX(Intersection inter)  { return inter.x + 84; }
    private int getNorthSlowX(Intersection inter) { return inter.x + 204; }
    private int getNorthFastX(Intersection inter) { return inter.x + 164; }
    private int getNorthEmgX(Intersection inter)  { return inter.x + 124; }

    private int getEastLaneForTurn(Intersection inter, Vehicle v) {
        if (v.getName().equals("Ambu") || v.getName().equals("Fire")) return EAST_LANE_EMG;
        if (v.getName().equals("Bike")) return EAST_LANE_SLOW;
        return EAST_LANE_FAST;
    }

    private int getWestLaneForTurn(Intersection inter, Vehicle v) {
        if (v.getName().equals("Ambu") || v.getName().equals("Fire")) return WEST_LANE_EMG;
        if (v.getName().equals("Bike")) return WEST_LANE_SLOW;
        return WEST_LANE_FAST;
    }

    private boolean isSpotOccupied(Vehicle v, double tx, double ty) {
        Rectangle testRect = new Rectangle((int)tx, (int)ty, v.getBodyWidth(), v.getBodyHeight());
        for (Vehicle o : vehicles) {
            if (o != v) {
                Rectangle oRect = new Rectangle((int)o.getX(), (int)o.getY(), o.getBodyWidth(), o.getBodyHeight());
                if (testRect.intersects(oRect)) {
                    if (o.isWaitingToTurn() || o.getSpeed() == 0) {
                        boolean vIsEmg = v.getName().equals("Ambu") || v.getName().equals("Fire");
                        boolean oIsEmg = o.getName().equals("Ambu") || o.getName().equals("Fire");
                        if (vIsEmg && !oIsEmg) continue;
                        if (vIsEmg == oIsEmg && System.identityHashCode(v) > System.identityHashCode(o)) continue;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public void executeTurn(Vehicle v) {
        v.setWaitingToTurn(false);
        for (Intersection inter : intersections) {
            if (inter.type.equals("5way")) {
                int cx = inter.x + roadWidth / 2;
                int cy = roadStartY + roadWidth / 2;
                if (v.getDirection() == Direction.EAST && v.getTurnIntent().equals("DIAGONAL") && !v.hasTurned()) {
                    if (Math.abs(v.getX() - (cx - 30)) <= v.getOriginalSpeed() * 2) {
                        double laneOffset = 25;
                        double tx = cx - 20 + laneOffset;
                        double ty = cy - 20 + laneOffset;
                        if (!isSpotOccupied(v, tx, ty)) {
                            v.setX(tx); v.setY(ty); v.setDirection(Direction.NORTHEAST); v.setHasTurned(true);
                            v.setWaitingToTurn(false);
                            return;
                        }
                    }
                }
                if (v.getDirection() == Direction.SOUTHWEST && !v.hasTurned()) {
                    if (Math.abs(v.getX() - (cx + 30)) <= v.getOriginalSpeed() * 2) {
                        Direction nextDir = new Random().nextBoolean() ? Direction.WEST : Direction.SOUTH;
                        double tx, ty;
                        if (nextDir == Direction.WEST) {
                            tx = cx - 40; ty = WEST_LANE_FAST;
                        } else {
                            tx = inter.x + 64; ty = cy + 40;
                        }
                        if (!isSpotOccupied(v, tx, ty)) {
                            v.setX(tx); v.setY(ty); v.setDirection(nextDir); v.setHasTurned(true);
                            v.setWaitingToTurn(false);
                        } else {
                            v.setWaitingToTurn(true);
                            v.setSpeed(0);
                        }
                        return;
                    }
                }
            }
        }

        for (Intersection inter : intersections) {
            if (v.hasTurned()) break;

            if (v.getDirection() == Direction.NORTH && inter.type.equals("3way") && v.getTurnIntent().equals("STRAIGHT")) {
                if (v.getX() >= inter.x && v.getX() <= inter.x + roadWidth) {
                    v.setTurnIntent(new Random().nextBoolean() ? "LEFT" : "RIGHT");
                }
            }

            if (v.getTurnIntent().equals("STRAIGHT")) continue;

            if (v.getDirection() == Direction.EAST || v.getDirection() == Direction.WEST) {
                if ((inter.type.equals("3way") && v.getTurnIntent().equals("LEFT") && v.getDirection() == Direction.EAST) ||
                        (inter.type.equals("3way") && v.getTurnIntent().equals("RIGHT") && v.getDirection() == Direction.WEST)) {
                    continue;
                }
                int[] southLanes = {getSouthSlowX(inter), getSouthFastX(inter), getSouthEmgX(inter)};
                int[] northLanes = {getNorthSlowX(inter), getNorthFastX(inter), getNorthEmgX(inter)};
                Integer targetX = null;
                Direction targetDir = null;
                boolean isEmergency = v.getName().equals("Ambu") || v.getName().equals("Fire");
                if (v.getDirection() == Direction.EAST) {
                    if (v.getTurnIntent().equals("RIGHT")) {
                        targetX = isEmergency ? southLanes[1] : (v.getName().equals("Bike") ? southLanes[0] : southLanes[1]);
                        targetDir = Direction.SOUTH;
                    } else if (v.getTurnIntent().equals("LEFT")) {
                        targetX = isEmergency ? northLanes[1] : (v.getName().equals("Bike") ? northLanes[0] : northLanes[1]);
                        targetDir = Direction.NORTH;
                    }
                } else if (v.getDirection() == Direction.WEST) {
                    if (v.getTurnIntent().equals("RIGHT")) {
                        targetX = isEmergency ? northLanes[1] : (v.getName().equals("Bike") ? northLanes[0] : northLanes[1]);
                        targetDir = Direction.NORTH;
                    } else if (v.getTurnIntent().equals("LEFT")) {
                        targetX = isEmergency ? southLanes[1] : (v.getName().equals("Bike") ? southLanes[0] : southLanes[1]);
                        targetDir = Direction.SOUTH;
                    }
                }
                if (targetX != null && targetDir != null) {
                    if (Math.abs(v.getX() - targetX) <= v.getOriginalSpeed()) {
                        if (!isSpotOccupied(v, targetX, v.getY())) {
                            v.setX(targetX);
                            v.setDirection(targetDir);
                            v.setHasTurned(true);
                        } else {
                            v.setWaitingToTurn(true);
                            v.setSpeed(0);
                        }
                    }
                }
            }
            else if (v.getDirection() == Direction.SOUTH) {
                if (v.getX() < inter.x || v.getX() > inter.x + roadWidth) continue;

                int targetY = -1;
                Direction targetDir = null;
                if (v.getTurnIntent().equals("RIGHT")) {
                    targetY = getWestLaneForTurn(inter, v);
                    targetDir = Direction.WEST;
                } else if (v.getTurnIntent().equals("LEFT")) {
                    targetY = getEastLaneForTurn(inter, v);
                    targetDir = Direction.EAST;
                }
                if (targetY != -1 && targetDir != null) {
                    if (Math.abs(v.getY() - targetY) <= v.getOriginalSpeed()) {
                        if (!isSpotOccupied(v, v.getX(), targetY)) {
                            v.setY(targetY);
                            v.setDirection(targetDir);
                            v.setHasTurned(true);
                        } else {
                            v.setWaitingToTurn(true);
                            v.setSpeed(0);
                        }
                    }
                }
            }
            else if (v.getDirection() == Direction.NORTH) {
                if (v.getX() < inter.x || v.getX() > inter.x + roadWidth) continue;

                int targetY = -1;
                Direction targetDir = null;
                if (v.getTurnIntent().equals("RIGHT")) {
                    targetY = getEastLaneForTurn(inter, v);
                    targetDir = Direction.EAST;
                } else if (v.getTurnIntent().equals("LEFT")) {
                    targetY = getWestLaneForTurn(inter, v);
                    targetDir = Direction.WEST;
                }
                if (targetY != -1 && targetDir != null) {
                    if (Math.abs(v.getY() - targetY) <= v.getOriginalSpeed()) {
                        if (!isSpotOccupied(v, v.getX(), targetY)) {
                            v.setY(targetY);
                            v.setDirection(targetDir);
                            v.setHasTurned(true);
                        } else {
                            v.setWaitingToTurn(true);
                            v.setSpeed(0);
                        }
                    }
                }
            }
        }
    }

    public boolean checkSafeDistance(Vehicle currentV, int threshold) {
        int cw = currentV.getBodyWidth(), ch = currentV.getBodyHeight();
        int offset = 6;

        Rectangle myActualRect = new Rectangle((int)currentV.getX() + 2, (int)currentV.getY() + 2, cw - 4, ch - 4);

        Rectangle predRect;
        switch (currentV.getDirection()) {
            case EAST: predRect = new Rectangle((int)currentV.getX(), (int)currentV.getY() + offset, cw + threshold, ch - 2*offset); break;
            case WEST: predRect = new Rectangle((int)currentV.getX() - threshold, (int)currentV.getY() + offset, cw + threshold, ch - 2*offset); break;
            case SOUTH: predRect = new Rectangle((int)currentV.getX() + offset, (int)currentV.getY(), cw - 2*offset, ch + threshold); break;
            case NORTH: predRect = new Rectangle((int)currentV.getX() + offset, (int)currentV.getY() - threshold, cw - 2*offset, ch + threshold); break;
            case NORTHEAST: predRect = new Rectangle((int)currentV.getX() + offset, (int)currentV.getY() - threshold, cw + threshold, ch + threshold); break;
            case SOUTHWEST: predRect = new Rectangle((int)currentV.getX() - threshold, (int)currentV.getY() + offset, cw + threshold, ch + threshold); break;
            default: predRect = new Rectangle((int)currentV.getX(), (int)currentV.getY(), cw, ch);
        }

        double myCx = currentV.getX() + cw/2.0;
        double myCy = currentV.getY() + ch/2.0;

        for (Vehicle other : vehicles) {
            if (other == currentV) continue;
            int shrink = 4;
            int ow = other.getBodyWidth(), oh = other.getBodyHeight();
            Rectangle otherRect = new Rectangle((int)other.getX() + shrink, (int)other.getY() + shrink, ow - 2*shrink, oh - 2*shrink);

            if (myActualRect.intersects(otherRect)) {
                if (System.identityHashCode(currentV) < System.identityHashCode(other)) return false;
            }

            if (predRect.intersects(otherRect)) {
                double otherCx = other.getX() + ow/2.0;
                double otherCy = other.getY() + oh/2.0;
                double dx = otherCx - myCx, dy = otherCy - myCy;
                double vx = 0, vy = 0;
                switch (currentV.getDirection()) {
                    case EAST: vx = 1; vy = 0; break;
                    case WEST: vx = -1; vy = 0; break;
                    case SOUTH: vx = 0; vy = 1; break;
                    case NORTH: vx = 0; vy = -1; break;
                    case NORTHEAST: vx = 1; vy = -1; break;
                    case SOUTHWEST: vx = -1; vy = 1; break;
                }
                double dot = dx * vx + dy * vy;

                if (dot > -0.1) {
                    if (currentV.getDirection() != other.getDirection()) {
                        if (System.identityHashCode(currentV) < System.identityHashCode(other)) return false;
                    } else {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean isLaneSafe(Vehicle currentV, double targetY) {
        int cw = currentV.getBodyWidth(), ch = currentV.getBodyHeight();
        Rectangle blindSpot = new Rectangle((int)currentV.getX() - 100, (int)targetY, cw + 200, ch);
        for (Vehicle other : vehicles) {
            if (other == currentV) continue;
            Rectangle otherRect = new Rectangle((int)other.getX(), (int)other.getY(), other.getBodyWidth(), other.getBodyHeight());
            if (blindSpot.intersects(otherRect)) return false;
        }
        return true;
    }

    private boolean isLaneSafeVertical(Vehicle currentV, double targetX) {
        Rectangle blindSpot = new Rectangle((int)targetX, (int)currentV.getY() - 100,
                currentV.getBodyWidth(), currentV.getBodyHeight() + 200);
        for (Vehicle other : vehicles) {
            if (other == currentV) continue;
            Rectangle otherRect = new Rectangle((int)other.getX(), (int)other.getY(),
                    other.getBodyWidth(), other.getBodyHeight());
            if (blindSpot.intersects(otherRect)) return false;
        }
        return true;
    }

    public void tryOvertake(Vehicle currentV) {
        if (!canChangeLane(currentV)) return;
        Direction d = currentV.getDirection();
        if (d == Direction.NORTHEAST || d == Direction.SOUTHWEST) return;
        boolean isEmergency = currentV.getName().equals("Ambu") || currentV.getName().equals("Fire");
        Double targetY = null;
        Double targetX = null;

        if (d == Direction.EAST) {
            if (currentV.getY() == EAST_LANE_SLOW) {
                if (isLaneSafe(currentV, EAST_LANE_FAST)) targetY = (double)EAST_LANE_FAST;
                else if (isEmergency && isLaneSafe(currentV, EAST_LANE_EMG)) targetY = (double)EAST_LANE_EMG;
            } else if (currentV.getY() == EAST_LANE_FAST && isEmergency && isLaneSafe(currentV, EAST_LANE_EMG)) targetY = (double)EAST_LANE_EMG;
        } else if (d == Direction.WEST) {
            if (currentV.getY() == WEST_LANE_SLOW) {
                if (isLaneSafe(currentV, WEST_LANE_FAST)) targetY = (double)WEST_LANE_FAST;
                else if (isEmergency && isLaneSafe(currentV, WEST_LANE_EMG)) targetY = (double)WEST_LANE_EMG;
            } else if (currentV.getY() == WEST_LANE_FAST && isEmergency && isLaneSafe(currentV, WEST_LANE_EMG)) targetY = (double)WEST_LANE_EMG;
        } else if (d == Direction.SOUTH || d == Direction.NORTH) {
            Intersection inter = getApproachingIntersection(currentV);
            if (inter == null) return;
            double curX = currentV.getX();
            if (d == Direction.SOUTH) {
                if (curX == getSouthSlowX(inter)) {
                    if (isLaneSafeVertical(currentV, getSouthFastX(inter))) targetX = (double) getSouthFastX(inter);
                    else if (isEmergency && isLaneSafeVertical(currentV, getSouthEmgX(inter))) targetX = (double) getSouthEmgX(inter);
                } else if (curX == getSouthFastX(inter) && isEmergency && isLaneSafeVertical(currentV, getSouthEmgX(inter))) {
                    targetX = (double) getSouthEmgX(inter);
                }
            } else { // NORTH
                if (curX == getNorthSlowX(inter)) {
                    if (isLaneSafeVertical(currentV, getNorthFastX(inter))) targetX = (double) getNorthFastX(inter);
                    else if (isEmergency && isLaneSafeVertical(currentV, getNorthEmgX(inter))) targetX = (double) getNorthEmgX(inter);
                } else if (curX == getNorthFastX(inter) && isEmergency && isLaneSafeVertical(currentV, getNorthEmgX(inter))) {
                    targetX = (double) getNorthEmgX(inter);
                }
            }
        }

        if (targetY != null) {
            currentV.setY(targetY);
            laneChangeCooldown.put(System.identityHashCode(currentV), ticks);
        } else if (targetX != null) {
            currentV.setX(targetX);
            laneChangeCooldown.put(System.identityHashCode(currentV), ticks);
        }
    }

    public void returnToSlowLane(Vehicle currentV) {
        if (justYielded.contains(System.identityHashCode(currentV))) return;
        if (!canChangeLane(currentV)) return;
        if (currentV.getName().equals("Ambu") || currentV.getName().equals("Fire")) return;
        Direction d = currentV.getDirection();
        if (d == Direction.NORTHEAST || d == Direction.SOUTHWEST) return;

        Double targetY = null;
        Double targetX = null;
        if (d == Direction.EAST) {
            if (currentV.getY() == EAST_LANE_EMG) targetY = (double)EAST_LANE_FAST;
            else if (currentV.getY() == EAST_LANE_FAST) targetY = (double)EAST_LANE_SLOW;
        } else if (d == Direction.WEST) {
            if (currentV.getY() == WEST_LANE_EMG) targetY = (double)WEST_LANE_FAST;
            else if (currentV.getY() == WEST_LANE_FAST) targetY = (double)WEST_LANE_SLOW;
        } else if (d == Direction.SOUTH || d == Direction.NORTH) {
            Intersection inter = getApproachingIntersection(currentV);
            if (inter == null) return;
            double curX = currentV.getX();
            if (d == Direction.SOUTH) {
                if (curX == getSouthEmgX(inter)) targetX = (double) getSouthFastX(inter);
                else if (curX == getSouthFastX(inter)) targetX = (double) getSouthSlowX(inter);
            } else { // NORTH
                if (curX == getNorthEmgX(inter)) targetX = (double) getNorthFastX(inter);
                else if (curX == getNorthFastX(inter)) targetX = (double) getNorthSlowX(inter);
            }
        }

        if (targetY != null && isLaneSafe(currentV, targetY)) {
            double oldY = currentV.getY();
            currentV.setY(targetY);
            if (!checkSafeDistance(currentV, 50)) currentV.setY(oldY);
            else laneChangeCooldown.put(System.identityHashCode(currentV), ticks);
        } else if (targetX != null && isLaneSafeVertical(currentV, targetX)) {
            double oldX = currentV.getX();
            currentV.setX(targetX);
            if (!checkSafeDistance(currentV, 50)) currentV.setX(oldX);
            else laneChangeCooldown.put(System.identityHashCode(currentV), ticks);
        }
    }

    private void yieldToEmergency(Vehicle currentV) {
        if (currentV.getName().equals("Ambu") || currentV.getName().equals("Fire")) return;
        Direction d = currentV.getDirection();
        if (d == Direction.NORTHEAST || d == Direction.SOUTHWEST) return;

        if (d == Direction.EAST || d == Direction.WEST) {
            boolean isLane2or3 = false;
            int currentLane = -1;
            if (d == Direction.EAST) {
                if (currentV.getY() == EAST_LANE_FAST) { isLane2or3 = true; currentLane = 2; }
                else if (currentV.getY() == EAST_LANE_EMG) { isLane2or3 = true; currentLane = 3; }
            } else if (d == Direction.WEST) {
                if (currentV.getY() == WEST_LANE_FAST) { isLane2or3 = true; currentLane = 2; }
                else if (currentV.getY() == WEST_LANE_EMG) { isLane2or3 = true; currentLane = 3; }
            }
            if (!isLane2or3) return;

            for (Vehicle other : vehicles) {
                if (other == currentV) continue;
                if (!(other.getName().equals("Ambu") || other.getName().equals("Fire"))) continue;
                if (other.getDirection() != currentV.getDirection()) continue;
                boolean sameLane = (Math.abs(currentV.getY() - other.getY()) < 5);
                if (!sameLane) continue;
                if (currentV.getDirection() == Direction.EAST && other.getX() > currentV.getX()) continue;
                if (currentV.getDirection() == Direction.WEST && other.getX() < currentV.getX()) continue;
                double dist = Math.abs(currentV.getX() - other.getX());
                if (dist > 150) continue;

                int targetLane = currentLane - 1;
                if (targetLane < 1) return;
                double targetY = -1;
                if (d == Direction.EAST) {
                    if (targetLane == 1) targetY = EAST_LANE_SLOW;
                    else if (targetLane == 2) targetY = EAST_LANE_FAST;
                } else {
                    if (targetLane == 1) targetY = WEST_LANE_SLOW;
                    else if (targetLane == 2) targetY = WEST_LANE_FAST;
                }
                if (targetY != -1 && isLaneSafe(currentV, targetY)) {
                    currentV.setY(targetY);
                    laneChangeCooldown.put(System.identityHashCode(currentV), ticks);
                    justYielded.add(System.identityHashCode(currentV));
                    break;
                }
            }
        }
        else if (d == Direction.SOUTH || d == Direction.NORTH) {
            Intersection inter = getApproachingIntersection(currentV);
            if (inter == null) return;
            boolean isLane2or3 = false;
            int currentLane = -1;
            double curX = currentV.getX();
            if (d == Direction.SOUTH) {
                if (curX == getSouthFastX(inter)) { isLane2or3 = true; currentLane = 2; }
                else if (curX == getSouthEmgX(inter)) { isLane2or3 = true; currentLane = 3; }
            } else { // NORTH
                if (curX == getNorthFastX(inter)) { isLane2or3 = true; currentLane = 2; }
                else if (curX == getNorthEmgX(inter)) { isLane2or3 = true; currentLane = 3; }
            }
            if (!isLane2or3) return;

            for (Vehicle other : vehicles) {
                if (other == currentV) continue;
                if (!(other.getName().equals("Ambu") || other.getName().equals("Fire"))) continue;
                if (other.getDirection() != d) continue;
                boolean sameLane = (Math.abs(currentV.getX() - other.getX()) < 5);
                if (!sameLane) continue;
                if (d == Direction.SOUTH && other.getY() < currentV.getY()) continue;
                if (d == Direction.NORTH && other.getY() > currentV.getY()) continue;
                double dist = Math.abs(currentV.getY() - other.getY());
                if (dist > 150) continue;

                int targetLane = currentLane - 1;
                if (targetLane < 1) return;
                double targetX = -1;
                if (d == Direction.SOUTH) {
                    if (targetLane == 1) targetX = getSouthSlowX(inter);
                    else if (targetLane == 2) targetX = getSouthFastX(inter);
                } else {
                    if (targetLane == 1) targetX = getNorthSlowX(inter);
                    else if (targetLane == 2) targetX = getNorthFastX(inter);
                }
                if (targetX != -1 && isLaneSafeVertical(currentV, targetX)) {
                    currentV.setX(targetX);
                    laneChangeCooldown.put(System.identityHashCode(currentV), ticks);
                    justYielded.add(System.identityHashCode(currentV));
                    break;
                }
            }
        }
    }

    // ======================== SỬA LỖI CHÍ MẠNG TẠI ĐÂY ========================
    private Intersection getApproachingIntersection(Vehicle v) {
        Direction d = v.getDirection();
        if (d == Direction.NORTHEAST) return null;

        double frontX = v.getX();
        double frontY = v.getY();

        switch (d) {
            case EAST:  frontX = v.getX() + v.getBodyWidth(); break;
            case SOUTH: frontY = v.getY() + v.getBodyHeight(); break;
            default: break;
        }

        int stopDist = 65;
        int westStopDist = 95;
        int overshoot = 30;

        for (Intersection inter : intersections) {
            // ĐƯỜNG NGANG (EAST/WEST): Bản chất kiểm tra X đã gắn liền với từng ngã tư cụ thể
            if (d == Direction.EAST && frontX >= inter.x - stopDist && frontX <= inter.x + overshoot) return inter;
            if (d == Direction.WEST && frontX <= inter.x + roadWidth + westStopDist && frontX >= inter.x + roadWidth - overshoot) return inter;

            // ĐƯỜNG DỌC (SOUTH/NORTH): PHẢI THÊM ĐIỀU KIỆN RÀ SOÁT TỌA ĐỘ TRỤC X CỦA XE
            // Đảm bảo xe đang đi đúng làn dọc của ngã tư đó chứ không quét nhầm sang ngã tư khác!
            if (d == Direction.SOUTH) {
                if (v.getX() >= inter.x && v.getX() <= inter.x + roadWidth) {
                    if (frontY >= roadStartY - stopDist && frontY <= roadStartY + overshoot) return inter;
                }
            }
            if (d == Direction.NORTH) {
                if (v.getX() >= inter.x && v.getX() <= inter.x + roadWidth) {
                    if (frontY <= roadStartY + roadWidth + stopDist && frontY >= roadStartY + roadWidth - overshoot) return inter;
                }
            }

            if (d == Direction.SOUTHWEST && inter.type.equals("5way")) {
                int cx = inter.x + roadWidth / 2;
                double targetStopX = cx + 200;
                if (frontX <= targetStopX + stopDist && frontX >= targetStopX - overshoot) {
                    return inter;
                }
            }
        }
        return null;
    }

    private void snapToStopLine(Vehicle v, Intersection inter) {
        int stopDist = 65;
        int westStopDist = 95;
        Direction d = v.getDirection();

        if (d == Direction.EAST) {
            v.setX(inter.x - stopDist - v.getBodyWidth());
        } else if (d == Direction.WEST) {
            v.setX(inter.x + roadWidth + westStopDist);
        } else if (d == Direction.SOUTH) {
            v.setY(roadStartY - stopDist - v.getBodyHeight());
        } else if (d == Direction.NORTH) {
            v.setY(roadStartY + roadWidth + stopDist);
        } else if (d == Direction.SOUTHWEST && inter.type.equals("5way")) {
            int cx = inter.x + roadWidth / 2;
            int cy = roadStartY + roadWidth / 2;
            double stopX = cx + 200;
            v.setX(stopX);
            v.setY(cy - (stopX - cx) - 85);
        }
    }

    private boolean isIntersectionClear(Vehicle currentV, Intersection inter) {
        Direction d = currentV.getDirection();
        if (d == Direction.SOUTH || d == Direction.NORTH || d == Direction.NORTHEAST || d == Direction.SOUTHWEST) return true;
        for (Vehicle other : vehicles) {
            if (other == currentV || currentV.getDirection() != other.getDirection()) continue;
            if (Math.abs(currentV.getY() - other.getY()) > 20) continue;
            if (d == Direction.EAST) {
                if (currentV.getX() < inter.x && other.getX() > currentV.getX()) {
                    if (other.getX() < inter.x + roadWidth + 15) return false;
                }
            } else if (d == Direction.WEST) {
                if (currentV.getX() > inter.x + roadWidth && other.getX() < currentV.getX()) {
                    if (other.getX() + other.getBodyWidth() > inter.x - 15) return false;
                }
            }
        }
        return true;
    }

    private boolean canVehicleGo(Vehicle v) {
        Intersection approaching = getApproachingIntersection(v);
        if (approaching == null) return true;
        if (v.getName().equals("Ambu") || v.getName().equals("Fire")) return true;
        boolean isGreen = approaching.light.canGo(v.getDirection());
        if (!isGreen) return false;
        return isIntersectionClear(v, approaching);
    }

    // ======================== UPDATE ALL ========================
    public void updateAll() {
        ticks++;
        for (Vehicle v : new java.util.ArrayList<>(vehicles)) {
            executeTurn(v);
            boolean lightAllows = canVehicleGo(v);

            if (!lightAllows) {
                Intersection inter = getApproachingIntersection(v);
                if (inter != null) {
                    if (checkSafeDistance(v, 60)) {
                        snapToStopLine(v, inter);
                    }
                }
            }

            boolean clearAheadFar = checkSafeDistance(v, 250);
            if (!clearAheadFar && lightAllows) tryOvertake(v);
            else if (clearAheadFar) returnToSlowLane(v);
            yieldToEmergency(v);
            boolean safe = checkSafeDistance(v, 35);
            if (safe && !v.isWaitingToTurn()) {
                v.move(lightAllows);
            }
        }
        if (ticks % 100 == 0) justYielded.clear();
        vehicles.removeIf(v -> v.getX() < -300 || v.getX() > width + 300 || v.getY() < -300 || v.getY() > height + 300);
    }

    public List<Vehicle> getVehicles() { return vehicles; }
}