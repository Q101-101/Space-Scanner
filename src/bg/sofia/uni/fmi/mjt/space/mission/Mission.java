package bg.sofia.uni.fmi.mjt.space.mission;

import bg.sofia.uni.fmi.mjt.space.rocket.RocketStatus;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public record Mission(String id, String company, String location,
                      LocalDate date, Detail detail, RocketStatus rocketStatus,
                      Optional<Double> cost, MissionStatus missionStatus) {

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Mission mission = (Mission) o;
        return Objects.equals(id, mission.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public String getCountry() {
        return location.replaceAll("\"", "")
                .substring(location.lastIndexOf(',') + 1)
                .trim();
    }

}